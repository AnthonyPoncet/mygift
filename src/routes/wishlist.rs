use crate::auth_middleware::AuthUser;
use crate::error_catcher::AppError;
use crate::managers::friends_manager::FriendsManager;
use crate::managers::wishlist_manager::{
    FriendWishList, WishList, WishlistManager, WishlistManagerError,
};
use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::Json;
use serde::Deserialize;
use std::collections::HashSet;

#[derive(Deserialize)]
pub(crate) struct AddCategory {
    name: String,
    share_with: Vec<i64>,
}

pub async fn add_category(
    State(wishlist_manager): State<WishlistManager>,
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Json(add_category): Json<AddCategory>,
) -> Result<StatusCode, AppError> {
    let all_users =
        build_category_user_list(auth_user.id, add_category.share_with, &friends_manager)?;
    wishlist_manager.add_category(&add_category.name, all_users)?;
    Ok(StatusCode::OK)
}

pub async fn edit_category(
    State(wishlist_manager): State<WishlistManager>,
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path(category_id): Path<i64>,
    Json(add_category): Json<AddCategory>,
) -> Result<StatusCode, AppError> {
    if !wishlist_manager.is_my_category(auth_user.id, category_id)? {
        return Err(AppError::Unauthorized);
    }

    let all_users =
        build_category_user_list(auth_user.id, add_category.share_with, &friends_manager)?;
    wishlist_manager.edit_category(category_id, &add_category.name, all_users)?;
    Ok(StatusCode::OK)
}

pub async fn delete_category(
    State(wishlist_manager): State<WishlistManager>,
    auth_user: AuthUser,
    Path(category_id): Path<i64>,
) -> Result<StatusCode, AppError> {
    if !wishlist_manager.is_my_category(auth_user.id, category_id)? {
        return Err(AppError::Unauthorized);
    }
    wishlist_manager.delete_category(auth_user.id, category_id)?;
    Ok(StatusCode::OK)
}

fn build_category_user_list(
    user_id: i64,
    share_with: Vec<i64>,
    friends_manager: &FriendsManager,
) -> Result<HashSet<i64>, AppError> {
    let mut all_users = HashSet::new();
    all_users.insert(user_id);
    if !share_with.is_empty() {
        let friends = friends_manager
            .get_friends(user_id)?
            .into_iter()
            .map(|u| u.id)
            .collect::<HashSet<_>>();
        for user_id in share_with {
            if !friends.contains(&user_id) {
                return Err(AppError::WishlistManager(
                    WishlistManagerError::UnknownUser(user_id),
                ));
            }
            all_users.insert(user_id);
        }
    }

    Ok(all_users)
}

#[derive(Deserialize)]
pub(crate) struct AddGift {
    name: String,
    description: Option<String>,
    price: Option<String>,
    where_to_buy: Option<String>,
    picture: Option<String>,
}

pub async fn add_gift(
    State(wishlist_manager): State<WishlistManager>,
    auth_user: AuthUser,
    Path(category_id): Path<i64>,
    Json(add_gift): Json<AddGift>,
) -> Result<StatusCode, AppError> {
    if !wishlist_manager.is_my_category(auth_user.id, category_id)? {
        return Err(AppError::Unauthorized);
    }
    wishlist_manager.add_gift(
        &add_gift.name,
        add_gift.description,
        add_gift.price,
        add_gift.where_to_buy,
        add_gift.picture,
        false,
        category_id,
    )?;
    Ok(StatusCode::OK)
}

pub async fn add_secret_gift(
    State(wishlist_manager): State<WishlistManager>,
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path((friend_id, category_id)): Path<(i64, i64)>,
    Json(add_gift): Json<AddGift>,
) -> Result<StatusCode, AppError> {
    if !friends_manager.is_my_friend(auth_user.id, friend_id)? {
        return Err(AppError::Unauthorized);
    }
    if !wishlist_manager.is_my_category(friend_id, category_id)? {
        return Err(AppError::Unauthorized);
    }
    wishlist_manager.add_gift(
        &add_gift.name,
        add_gift.description,
        add_gift.price,
        add_gift.where_to_buy,
        add_gift.picture,
        true,
        category_id,
    )?;
    Ok(StatusCode::OK)
}

pub async fn edit_gift(
    State(wishlist_manager): State<WishlistManager>,
    auth_user: AuthUser,
    Path((category_id, gift_id)): Path<(i64, i64)>,
    Json(add_gift): Json<AddGift>,
) -> Result<StatusCode, AppError> {
    if !wishlist_manager.is_my_gift_for_edit(auth_user.id, gift_id)? {
        return Err(AppError::Unauthorized);
    }
    if !wishlist_manager.is_my_category(auth_user.id, category_id)? {
        return Err(AppError::Unauthorized);
    }
    wishlist_manager.edit_gift(
        gift_id,
        &add_gift.name,
        add_gift.description,
        add_gift.price,
        add_gift.where_to_buy,
        add_gift.picture,
        category_id,
    )?;
    Ok(StatusCode::OK)
}
pub async fn edit_secret_gift(
    State(wishlist_manager): State<WishlistManager>,
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path((friend_id, category_id, gift_id)): Path<(i64, i64, i64)>,
    Json(add_gift): Json<AddGift>,
) -> Result<StatusCode, AppError> {
    if !friends_manager.is_my_friend(auth_user.id, friend_id)? {
        return Err(AppError::Unauthorized);
    }
    if !wishlist_manager.is_my_gift_for_edit(friend_id, gift_id)? {
        return Err(AppError::Unauthorized);
    }
    if !wishlist_manager.is_my_category(friend_id, category_id)? {
        return Err(AppError::Unauthorized);
    }
    wishlist_manager.edit_gift(
        gift_id,
        &add_gift.name,
        add_gift.description,
        add_gift.price,
        add_gift.where_to_buy,
        add_gift.picture,
        category_id,
    )?;
    Ok(StatusCode::OK)
}

pub async fn delete_gift(
    State(wishlist_manager): State<WishlistManager>,
    auth_user: AuthUser,
    Path((category_id, gift_id)): Path<(i64, i64)>,
) -> Result<StatusCode, AppError> {
    if !wishlist_manager.is_my_gift(auth_user.id, category_id, gift_id)? {
        return Err(AppError::Unauthorized);
    }
    wishlist_manager.delete_gift(gift_id)?;
    Ok(StatusCode::OK)
}

pub async fn delete_secret_gift(
    State(wishlist_manager): State<WishlistManager>,
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path((friend_id, category_id, gift_id)): Path<(i64, i64, i64)>,
) -> Result<StatusCode, AppError> {
    if !friends_manager.is_my_friend(auth_user.id, friend_id)? {
        return Err(AppError::Unauthorized);
    }
    if !wishlist_manager.is_my_gift(friend_id, category_id, gift_id)? {
        return Err(AppError::Unauthorized);
    }
    wishlist_manager.delete_gift(gift_id)?;
    Ok(StatusCode::OK)
}

pub async fn change_heart_gift(
    State(wishlist_manager): State<WishlistManager>,
    auth_user: AuthUser,
    Path((category_id, gift_id)): Path<(i64, i64)>,
) -> Result<StatusCode, AppError> {
    if !wishlist_manager.is_my_gift(auth_user.id, category_id, gift_id)? {
        return Err(AppError::Unauthorized);
    }
    wishlist_manager.change_heart_gift(gift_id)?;
    Ok(StatusCode::OK)
}

pub async fn reserve_gift(
    State(wishlist_manager): State<WishlistManager>,
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path((friend_id, gift_id)): Path<(i64, i64)>,
) -> Result<StatusCode, AppError> {
    if !friends_manager.is_my_friend(auth_user.id, friend_id)? {
        return Err(AppError::Unauthorized);
    }
    if wishlist_manager.is_gift_reserved(gift_id)? {
        return Err(AppError::Conflict);
    }

    wishlist_manager.reserve_gift(gift_id, Some(auth_user.id))?;
    Ok(StatusCode::OK)
}

pub async fn unreserve_gift(
    State(wishlist_manager): State<WishlistManager>,
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path((friend_id, gift_id)): Path<(i64, i64)>,
) -> Result<StatusCode, AppError> {
    if !friends_manager.is_my_friend(auth_user.id, friend_id)? {
        return Err(AppError::Unauthorized);
    }
    if !wishlist_manager.is_gift_reserved_by_me(gift_id, auth_user.id)? {
        return Err(AppError::Unauthorized);
    }

    wishlist_manager.reserve_gift(gift_id, None)?;
    Ok(StatusCode::OK)
}

pub async fn get_my_wishlist(
    State(wishlist_manager): State<WishlistManager>,
    auth_user: AuthUser,
) -> Result<(StatusCode, Json<WishList>), AppError> {
    let wishlist = wishlist_manager.get_my_wishlist(auth_user.id)?;
    Ok((StatusCode::OK, Json(wishlist)))
}

pub async fn get_friend_wishlist(
    State(wishlist_manager): State<WishlistManager>,
    State(friends_manager): State<FriendsManager>,
    auth_user: AuthUser,
    Path(friend_id): Path<i64>,
) -> Result<(StatusCode, Json<FriendWishList>), AppError> {
    if !friends_manager.is_my_friend(auth_user.id, friend_id)? {
        return Err(AppError::Unauthorized);
    }
    let wishlist = wishlist_manager.get_friend_wishlist(auth_user.id, friend_id)?;
    Ok((StatusCode::OK, Json(wishlist)))
}
