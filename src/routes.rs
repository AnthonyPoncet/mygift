use crate::auth_middleware::AuthLayer;
use crate::configuration::Configuration;
use crate::managers::events_manager::EventsManager;
use crate::managers::friends_manager::FriendsManager;
use crate::managers::jwt_manager::JwtManager;
use crate::managers::session_manager::SessionManager;
use crate::managers::users_manager::UsersManager;
use crate::managers::wishlist_manager::WishlistManager;
use crate::routes::connection::{change_account, login, logout, reset_password};
use crate::routes::events::get_events;
use crate::routes::files::{get_file, upload_file};
use crate::routes::friends::{
    accept_request, add_friend, cancel_request, decline_request, get_friend_id, get_friends,
    get_requests,
};
use crate::routes::users::{create_user, edit_user};
use crate::routes::wishlist::{
    add_category, add_gift, add_secret_gift, change_heart_gift, delete_category, delete_gift,
    delete_secret_gift, edit_category, edit_gift, edit_secret_gift, get_friend_wishlist,
    get_my_wishlist, reorder_categories, reorder_gifts, reserve_gift, unreserve_gift,
};
use axum::extract::FromRef;
use axum::routing::{delete, get, patch, post, put};
use axum::Router;
use std::sync::Arc;
use tower_cookies::CookieManagerLayer;

mod connection;
mod events;
pub mod files;
mod friends;
mod users;
mod wishlist;

#[derive(Clone)]
pub(crate) struct AppState {
    pub(crate) users_manager: UsersManager,
    pub(crate) jwt_manager: Arc<JwtManager>,
    pub(crate) session_manager: SessionManager,
    pub(crate) events_manager: EventsManager,
    pub(crate) friends_manager: FriendsManager,
    pub(crate) wishlist_manager: WishlistManager,

    pub(crate) configuration: Arc<Configuration>,
}

impl FromRef<AppState> for UsersManager {
    fn from_ref(app_state: &AppState) -> UsersManager {
        app_state.users_manager.clone()
    }
}

impl FromRef<AppState> for Arc<JwtManager> {
    fn from_ref(app_state: &AppState) -> Arc<JwtManager> {
        app_state.jwt_manager.clone()
    }
}

impl FromRef<AppState> for SessionManager {
    fn from_ref(app_state: &AppState) -> SessionManager {
        app_state.session_manager.clone()
    }
}

impl FromRef<AppState> for EventsManager {
    fn from_ref(app_state: &AppState) -> EventsManager {
        app_state.events_manager.clone()
    }
}

impl FromRef<AppState> for FriendsManager {
    fn from_ref(app_state: &AppState) -> FriendsManager {
        app_state.friends_manager.clone()
    }
}

impl FromRef<AppState> for WishlistManager {
    fn from_ref(app_state: &AppState) -> WishlistManager {
        app_state.wishlist_manager.clone()
    }
}

impl FromRef<AppState> for Arc<Configuration> {
    fn from_ref(app_state: &AppState) -> Arc<Configuration> {
        app_state.configuration.clone()
    }
}

pub(crate) fn create_api_routes(
    session_manager: SessionManager,
    jwt_manager: Arc<JwtManager>,
    configuration: Arc<Configuration>,
) -> Router<AppState> {
    Router::new()
        .route("/events", get(get_events))
        .route("/users/change-account", post(change_account))
        .route("/users/logout", get(logout))
        .route("/users", patch(edit_user))
        .route("/friends", post(add_friend))
        .route("/friends", get(get_friends))
        .route("/friends/{friend_name}", get(get_friend_id))
        .route("/friends/requests", get(get_requests))
        .route("/friends/requests/{request_id}/accept", get(accept_request))
        .route(
            "/friends/requests/{request_id}/decline",
            get(decline_request),
        )
        .route("/friends/requests/{request_id}", delete(cancel_request))
        .route("/wishlist", get(get_my_wishlist))
        .route("/wishlist/categories", post(add_category))
        .route("/wishlist/categories/reorder", patch(reorder_categories))
        .route("/wishlist/categories/{category_id}", patch(edit_category))
        .route(
            "/wishlist/categories/{category_id}/reorder",
            patch(reorder_gifts),
        )
        .route(
            "/wishlist/categories/{category_id}",
            delete(delete_category),
        )
        .route("/wishlist/categories/{category_id}/gifts", post(add_gift))
        .route(
            "/wishlist/categories/{category_id}/gifts/{gift_id}",
            patch(edit_gift),
        )
        .route(
            "/wishlist/categories/{category_id}/gifts/{gift_id}",
            delete(delete_gift),
        )
        .route(
            "/wishlist/categories/{category_id}/gifts/{gift_id}/change_like",
            get(change_heart_gift),
        )
        .route("/wishlist/friend/{friend_id}", get(get_friend_wishlist))
        .route(
            "/wishlist/friend/{friend_id}/categories/{category_id}/gifts",
            post(add_secret_gift),
        )
        .route(
            "/wishlist/friend/{friend_id}/categories/{category_id}/gifts/{gift_id}",
            patch(edit_secret_gift),
        )
        .route(
            "/wishlist/friend/{friend_id}/categories/{category_id}/gifts/{gift_id}",
            delete(delete_secret_gift),
        )
        .route(
            "/wishlist/friend/{friend_id}/gifts/{gift_id}",
            post(reserve_gift),
        )
        .route(
            "/wishlist/friend/{friend_id}/gifts/{gift_id}",
            delete(unreserve_gift),
        )
        .route("/files/{file_name}", get(get_file))
        .route("/files", post(upload_file))
        .layer(AuthLayer {
            session_manager,
            jwt_manager,
            configuration,
        })
        .route("/users/connect", post(login))
        .route("/users/password-reset", post(reset_password))
        .route("/users", put(create_user))
        .layer(CookieManagerLayer::new())
}
