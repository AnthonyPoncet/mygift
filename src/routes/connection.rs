use crate::auth_middleware::AuthUser;
use crate::error_catcher::AppError;
use crate::managers::jwt_manager::JwtManager;
use crate::managers::session_manager::SessionManager;
use crate::managers::users_manager::UsersManager;
use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tower_cookies::cookie::SameSite;
use tower_cookies::{Cookie, Cookies};
use tracing::log::error;

#[derive(Deserialize)]
pub(crate) struct LoginDetail {
    name: String,
    password: String,
}

#[derive(Serialize)]
pub(crate) struct LoginResponse {
    pub(crate) id: i64,
    pub(crate) name: String,
    pub(crate) token: String,
    pub(crate) picture: Option<String>,
    pub(crate) date_of_birth: Option<i64>,
}

pub(crate) async fn login(
    State(users_manager): State<UsersManager>,
    State(jwt_manager): State<Arc<JwtManager>>,
    State(mut session_manager): State<SessionManager>,
    cookies: Cookies,
    login_detail: Json<LoginDetail>,
) -> Result<(StatusCode, Json<LoginResponse>), AppError> {
    let clean_user = users_manager.check_password(&login_detail.name, &login_detail.password)?;
    let session = session_manager.generate_session(clean_user.id);
    cookies.add(
        Cookie::build(("SESSION", session))
            .secure(true)
            .http_only(true)
            .path("/api")
            .same_site(SameSite::Strict)
            .build(),
    );
    let jwt = jwt_manager.generate_jwt(clean_user.id)?;
    Ok((
        StatusCode::OK,
        Json(LoginResponse {
            id: clean_user.id,
            name: clean_user.name,
            token: jwt,
            picture: clean_user.picture,
            date_of_birth: clean_user.date_of_birth,
        }),
    ))
}

pub(crate) async fn change_account(
    State(users_manager): State<UsersManager>,
    State(jwt_manager): State<Arc<JwtManager>>,
    State(mut session_manager): State<SessionManager>,
    auth_user: AuthUser,
    login_detail: Json<LoginDetail>,
) -> Result<(StatusCode, Json<LoginResponse>), AppError> {
    let clean_user = users_manager.check_password(&login_detail.name, &login_detail.password)?;
    if session_manager
        .set_common_session(clean_user.id, auth_user.id)
        .is_none()
    {
        error!("Try to change account from another user that does not have a session");
        return Err(AppError::Unauthorized);
    };
    let jwt = jwt_manager.generate_jwt(clean_user.id)?;
    Ok((
        StatusCode::OK,
        Json(LoginResponse {
            id: clean_user.id,
            name: clean_user.name,
            token: jwt,
            picture: clean_user.picture,
            date_of_birth: clean_user.date_of_birth,
        }),
    ))
}

pub(crate) async fn logout(
    State(session_manager): State<SessionManager>,
    auth_user: AuthUser,
) -> Result<StatusCode, AppError> {
    session_manager.delete_session(auth_user.id);
    Ok(StatusCode::ACCEPTED)
}

#[derive(Deserialize)]
pub(crate) struct PasswordReset {
    name: String,
    password: String,
    uuid: String,
}

pub(crate) async fn reset_password(
    State(users_manager): State<UsersManager>,
    password_reset: Json<PasswordReset>,
) -> Result<StatusCode, AppError> {
    let user = users_manager.get_user(&password_reset.name)?;
    users_manager.change_user_password(user.id, &password_reset.uuid, &password_reset.password)?;
    Ok(StatusCode::ACCEPTED)
}
