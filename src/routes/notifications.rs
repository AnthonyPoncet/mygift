use crate::auth_middleware::AuthUser;
use crate::error_catcher::AppError;
use crate::managers::notifications_manager::{Notification, NotificationsManager};
use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;

pub(crate) async fn get_notifications(
    State(notification_manager): State<NotificationsManager>,
    auth_user: AuthUser,
) -> Result<(StatusCode, Json<Vec<Notification>>), AppError> {
    Ok((
        StatusCode::OK,
        Json(notification_manager.get_notifications(auth_user.id)?),
    ))
}

pub(crate) async fn read_notification(
    State(notification_manager): State<NotificationsManager>,
    _auth_user: AuthUser,
    Path(notification_id): Path<i64>,
) -> Result<StatusCode, AppError> {
    notification_manager.read_notification(notification_id)?;
    Ok(StatusCode::OK)
}
