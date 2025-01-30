use crate::auth_middleware::AuthUser;
use crate::error_catcher::AppError;
use crate::managers::events_manager::{Event, EventsManager};
use axum::extract::State;
use axum::http::StatusCode;
use axum::Json;

pub(crate) async fn get_events(
    State(events_manager): State<EventsManager>,
    auth_user: AuthUser,
) -> Result<(StatusCode, Json<Vec<Event>>), AppError> {
    Ok((
        StatusCode::OK,
        Json(events_manager.get_events(auth_user.id)?),
    ))
}
