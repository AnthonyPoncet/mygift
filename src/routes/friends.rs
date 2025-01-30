use crate::auth_middleware::AuthUser;
use crate::error_catcher::AppError;
use crate::managers::friends_manager::{FriendsManager, RequestStatus, Requests};
use crate::managers::users_manager::CleanUser;
use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use serde::{Deserialize, Serialize};

#[derive(Deserialize)]
pub(crate) struct AddFriend {
    name: String,
}

pub async fn add_friend(
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Json(add_friend): Json<AddFriend>,
) -> Result<StatusCode, AppError> {
    friends_manager.create_friend_request(auth_user.id, &add_friend.name)?;
    Ok(StatusCode::OK)
}

#[derive(Serialize)]
pub(crate) struct Friends {
    friends: Vec<Friend>,
}
#[derive(Serialize)]
pub(crate) struct Friend {
    id: i64,
    name: String,
    picture: Option<String>,
    date_of_birth: Option<i64>,
}

impl From<Vec<CleanUser>> for Friends {
    fn from(value: Vec<CleanUser>) -> Self {
        let friends = value
            .into_iter()
            .map(|u| Friend {
                id: u.id,
                name: u.name,
                picture: u.picture,
                date_of_birth: u.date_of_birth,
            })
            .collect();
        Friends { friends }
    }
}

pub async fn get_friends(
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
) -> Result<(StatusCode, Json<Friends>), AppError> {
    let friends = friends_manager.get_friends(auth_user.id)?;
    Ok((StatusCode::OK, Json(friends.into())))
}

#[derive(Serialize)]
pub(crate) struct FriendId {
    id: i64,
}
pub async fn get_friend_id(
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path(friend_name): Path<String>,
) -> Result<(StatusCode, Json<FriendId>), AppError> {
    let friend_id = friends_manager.get_friend_id(auth_user.id, &friend_name)?;
    Ok((StatusCode::OK, Json(FriendId { id: friend_id })))
}

pub async fn get_requests(
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
) -> Result<(StatusCode, Json<Requests>), AppError> {
    let requests = friends_manager.get_requests(auth_user.id)?;
    Ok((StatusCode::OK, Json(requests)))
}

pub async fn accept_request(
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path(request_id): Path<i64>,
) -> Result<StatusCode, AppError> {
    friends_manager.update_received_request(request_id, auth_user.id, RequestStatus::Accepted)?;
    Ok(StatusCode::OK)
}

pub async fn decline_request(
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path(request_id): Path<i64>,
) -> Result<StatusCode, AppError> {
    friends_manager.update_received_request(request_id, auth_user.id, RequestStatus::Declined)?;
    Ok(StatusCode::OK)
}

pub async fn cancel_request(
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path(request_id): Path<i64>,
) -> Result<StatusCode, AppError> {
    friends_manager.cancel_sent_request(request_id, auth_user.id)?;
    Ok(StatusCode::OK)
}
