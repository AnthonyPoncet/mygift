use crate::configuration::Configuration;
use crate::error_catcher::AppError;
use crate::managers::jwt_manager::JwtManager;
use crate::managers::session_manager::SessionManager;
use axum::extract::FromRequestParts;
use axum::extract::Request;
use axum::http;
use axum::http::request::Parts;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use futures_util::future::BoxFuture;
use std::sync::Arc;
use std::task::{Context, Poll};
use tower::{Layer, Service};
use tower_cookies::Cookies;
use tracing::debug;
use tracing::log::error;

#[derive(Clone)]
pub struct AuthLayer {
    pub session_manager: SessionManager,
    pub jwt_manager: Arc<JwtManager>,
    pub configuration: Arc<Configuration>,
}

impl<S> Layer<S> for AuthLayer {
    type Service = AuthMiddleware<S>;

    fn layer(&self, inner: S) -> Self::Service {
        AuthMiddleware {
            inner,
            layer: self.clone(),
        }
    }
}

#[derive(Clone)]
pub struct AuthMiddleware<S> {
    inner: S,
    layer: AuthLayer,
}

impl<S> Service<Request> for AuthMiddleware<S>
where
    S: Service<Request, Response = Response> + Send + 'static,
    S::Future: Send + 'static,
{
    type Response = S::Response;
    type Error = S::Error;
    type Future = BoxFuture<'static, Result<Self::Response, Self::Error>>;

    fn poll_ready(&mut self, cx: &mut Context<'_>) -> Poll<Result<(), Self::Error>> {
        self.inner.poll_ready(cx)
    }

    fn call(&mut self, request: Request) -> Self::Future {
        let session = if self.layer.configuration.debug {
            "DEBUG".to_string()
        } else {
            let cookies = request
                .extensions()
                .get::<Cookies>()
                .expect("Missing cookie layer to work");
            let Some(session) = cookies.get("SESSION") else {
                error!("Did not get the session cookie");
                return Self::unauthorized();
            };
            let session = session.value().to_string();
            debug!("Got cookie: {session}");
            session
        };

        let (parts, body) = request.into_parts();
        let Some(jwt_token) = parts.headers.get("Authorization") else {
            error!("Did not get the Authorization header");
            return Self::unauthorized();
        };
        let jwt_token = match jwt_token.to_str() {
            Ok(t) => t,
            Err(e) => {
                error!("Cannot process Authorization header: {e}");
                return Self::unauthorized();
            }
        };

        if !jwt_token.starts_with("Bearer ") {
            error!("Authorization header does not start by Bearer");
            return Self::unauthorized();
        }

        let jwt_token = &jwt_token[7..];
        debug!("Got token: {jwt_token}");
        let claims = match self.layer.jwt_manager.verify_jwt(jwt_token) {
            Ok(c) => c,
            Err(e) => {
                error!("Cannot decode JWT: {e}");
                return Self::unauthorized();
            }
        };
        debug!("Processing user {}", claims.id);

        let Some(current_session) = self.layer.session_manager.get_session(claims.id) else {
            error!("No session available for user {}", claims.id);
            return Self::unauthorized();
        };

        if !self.layer.configuration.debug && current_session != session {
            error!("Session mismatch: received {session} while expecting {current_session}");
            return Self::unauthorized();
        }

        let mut request = Request::from_parts(parts, body);
        request.extensions_mut().insert(AuthUser { id: claims.id });
        let future = self.inner.call(request);
        Box::pin(async move {
            let response: Response = future.await?;
            Ok(response)
        })
    }
}

impl<S> AuthMiddleware<S>
where
    S: Service<Request, Response = Response> + Send + 'static,
    S::Future: Send + 'static,
{
    fn unauthorized() -> BoxFuture<'static, Result<S::Response, S::Error>> {
        Box::pin(async { Ok(AppError::Unauthorized.into_response()) })
    }
}

#[derive(Clone)]
pub struct AuthUser {
    pub id: i64,
}

impl<S> FromRequestParts<S> for AuthUser
where
    S: Sync + Send,
{
    type Rejection = (http::StatusCode, &'static str);

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        parts.extensions.get::<AuthUser>().cloned().ok_or((
            StatusCode::INTERNAL_SERVER_ERROR,
            "Can't extract AuthUser. Is `AuthLayer` enabled?",
        ))
    }
}
