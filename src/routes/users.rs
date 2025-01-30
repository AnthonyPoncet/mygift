use crate::auth_middleware::AuthUser;
use crate::error_catcher::AppError;
use crate::managers::jwt_manager::JwtManager;
use crate::managers::session_manager::SessionManager;
use crate::managers::users_manager::UsersManager;
use crate::routes::connection::LoginResponse;
use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;
use serde::Deserialize;
use std::sync::Arc;
use tower_cookies::cookie::SameSite;
use tower_cookies::{Cookie, Cookies};

#[derive(Deserialize)]
pub(crate) struct CreateUser {
    name: String,
    password: String,
}

pub(crate) async fn create_user(
    State(users_manager): State<UsersManager>,
    State(jwt_manager): State<Arc<JwtManager>>,
    State(mut session_manager): State<SessionManager>,
    cookies: Cookies,
    create_user: Json<CreateUser>,
) -> Result<(StatusCode, Json<LoginResponse>), AppError> {
    let user_id = users_manager.add_user(&create_user.name, &create_user.password)?;
    let session = session_manager.generate_session(user_id);
    cookies.add(
        Cookie::build(("SESSION", session))
            .http_only(true)
            .path("/api")
            .same_site(SameSite::Strict)
            .build(),
    );
    let jwt = jwt_manager.generate_jwt(user_id)?;
    Ok((
        StatusCode::OK,
        Json(LoginResponse {
            id: user_id,
            name: create_user.name.to_string(),
            token: jwt,
            picture: None,
            date_of_birth: None,
        }),
    ))
}

#[derive(Deserialize)]
pub(crate) struct EditUser {
    name: String,
    picture: Option<String>,
    date_of_birth: Option<i64>,
}

pub(crate) async fn edit_user(
    State(users_manager): State<UsersManager>,
    auth_user: AuthUser,
    edit_user: Json<EditUser>,
) -> Result<StatusCode, AppError> {
    users_manager.edit_user(
        auth_user.id,
        &edit_user.name,
        &edit_user.picture,
        &edit_user.date_of_birth,
    )?;
    Ok(StatusCode::OK)
}
