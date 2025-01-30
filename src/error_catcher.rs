use crate::managers::events_manager::EventsManagerError;
use crate::managers::friends_manager::FriendsManagerError;
use crate::managers::jwt_manager::JwtManagerError;
use crate::managers::users_manager::UsersManagerError;
use crate::managers::wishlist_manager::WishlistManagerError;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use serde::Serialize;
use tracing::log::error;

#[derive(thiserror::Error, Debug)]
#[error(transparent)]
pub(crate) enum AppError {
    UsersManager(#[from] UsersManagerError),
    JwtManager(#[from] JwtManagerError),
    EventsManager(#[from] EventsManagerError),
    FriendsManager(#[from] FriendsManagerError),
    WishlistManager(#[from] WishlistManagerError),
    #[error("Unauthorized")]
    Unauthorized,
    #[error("Conflict")]
    Conflict,
}

#[derive(Serialize)]
pub(crate) struct ErrorJson {
    message: String,
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        match self {
            AppError::UsersManager(UsersManagerError::PasswordMismatch)
            | AppError::UsersManager(UsersManagerError::UnknownUser(_))
            | AppError::Unauthorized => StatusCode::UNAUTHORIZED.into_response(),
            AppError::UsersManager(UsersManagerError::UserAlreadyExist(_))
            | AppError::FriendsManager(FriendsManagerError::FriendRequestAlreadyExists(_, _))
            | AppError::FriendsManager(FriendsManagerError::CannotAskYourself(_))
            | AppError::Conflict => StatusCode::CONFLICT.into_response(),
            AppError::FriendsManager(FriendsManagerError::UnknownUser(_))
            | AppError::FriendsManager(FriendsManagerError::FriendRequestDoesNotExists(_, _)) => {
                StatusCode::NOT_FOUND.into_response()
            }
            _ => {
                error!("Got an error {self}");
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    Json(ErrorJson {
                        message: self.to_string(),
                    }),
                )
                    .into_response()
            }
        }
    }
}
