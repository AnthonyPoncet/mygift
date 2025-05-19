use rusqlite::{params, Connection, OptionalExtension, Row, Transaction};
use serde::Serialize;
use std::collections::HashSet;
use std::sync::{Arc, Mutex};

#[derive(Clone)]
pub struct WishlistManager {
    connection: Arc<Mutex<Connection>>,
}

#[derive(thiserror::Error, Debug)]
#[error(transparent)]
pub(crate) enum WishlistManagerError {
    Sqlite(#[from] rusqlite::Error),
    #[error("Unknown user {0}")]
    UnknownUser(i64),
}

impl WishlistManager {
    pub fn new(connection: Arc<Mutex<Connection>>) -> Result<Self, WishlistManagerError> {
        Self::init_database(&connection)?;
        Ok(Self { connection })
    }

    fn init_database(connection: &Arc<Mutex<Connection>>) -> Result<(), WishlistManagerError> {
        let connection = connection.lock().unwrap();
        connection.execute_batch("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)")?;
        connection.execute_batch("CREATE TABLE IF NOT EXISTS joinUserAndCategory (userId INTEGER NOT NULL, categoryId INTEGER NOT NULL, rank INTEGER NOT NULL, \
            FOREIGN KEY(userId) REFERENCES users(id), FOREIGN KEY(categoryId) REFERENCES categories(id))")?;
        connection.execute_batch("CREATE TABLE IF NOT EXISTS gifts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT, price TEXT, \
            whereToBuy TEXT, picture TEXT, secret INTEGER NOT NULL, heart INTEGER NOT NULL, rank INTEGER NOT NULL, reservedBy INTEGER, categoryId INTEGER NOT NULL, \
            FOREIGN KEY(reservedBy) REFERENCES users(id), FOREIGN KEY(categoryId) REFERENCES categories(id))")?;
        Ok(())
    }

    pub fn add_category(
        &self,
        name: &str,
        user_ids: HashSet<i64>,
    ) -> Result<i64, WishlistManagerError> {
        let mut connection = self.connection.lock().unwrap();

        let transaction = connection.transaction()?;

        transaction.execute("INSERT INTO categories (name) VALUES (?)", params![name])?;
        let category_id = transaction.last_insert_rowid();
        Self::add_user_to_category(&transaction, category_id, user_ids)?;
        transaction.commit()?;

        Ok(category_id)
    }

    fn add_user_to_category(
        transaction: &Transaction,
        category_id: i64,
        user_ids: HashSet<i64>,
    ) -> Result<(), WishlistManagerError> {
        for user_id in user_ids {
            let rank = transaction
                .query_row(
                    "SELECT MAX(rank) FROM joinUserAndCategory WHERE userId=?",
                    params![user_id],
                    |row| row.get::<_, Option<i64>>(0),
                )?
                .unwrap_or(-1);
            transaction.execute(
                "INSERT INTO joinUserAndCategory(userId, categoryId, rank) VALUES (?,?,?)",
                params![user_id, category_id, rank + 1],
            )?;
        }
        Ok(())
    }

    pub fn edit_category(
        &self,
        category_id: i64,
        name: &str,
        mut user_ids: HashSet<i64>,
    ) -> Result<(), WishlistManagerError> {
        let mut connection = self.connection.lock().unwrap();
        let mut current_users = HashSet::new();
        {
            let mut statement =
                connection.prepare("SELECT userId FROM joinUserAndCategory WHERE categoryId=?")?;
            let rows = statement.query_map(params![category_id], |row| row.get(0))?;
            for row in rows {
                current_users.insert(row?);
            }
        }
        let mut to_delete = Vec::new();
        for user in current_users {
            if !user_ids.remove(&user) {
                to_delete.push(user);
            }
        }

        let transaction = connection.transaction()?;
        transaction.execute(
            "UPDATE categories SET name=? WHERE id=?",
            params![name, category_id],
        )?;
        Self::add_user_to_category(&transaction, category_id, user_ids)?;
        for user_id in to_delete {
            transaction.execute(
                "DELETE FROM joinUserAndCategory WHERE userId=? AND categoryId=?",
                params![user_id, category_id],
            )?;
        }
        transaction.commit()?;

        Ok(())
    }

    pub fn reorder_categories(
        &self,
        user_id: i64,
        starting_rank: usize,
        categories: &[i64],
    ) -> Result<(), WishlistManagerError> {
        let mut connection = self.connection.lock().unwrap();
        let transaction = connection.transaction()?;
        for (index, category) in categories.iter().enumerate() {
            transaction.execute(
                "UPDATE joinUserAndCategory SET rank=? WHERE userId=? AND categoryId=?",
                params![index + starting_rank, user_id, category],
            )?;
        }
        transaction.commit()?;

        Ok(())
    }

    pub fn delete_category(
        &self,
        user_id: i64,
        category_id: i64,
    ) -> Result<(), WishlistManagerError> {
        let mut connection = self.connection.lock().unwrap();

        let transaction = connection.transaction()?;
        transaction.execute(
            "DELETE FROM joinUserAndCategory WHERE userId=? AND categoryId=?",
            params![user_id, category_id],
        )?;
        let count = transaction.query_row(
            "SELECT COUNT(userId) FROM joinUserAndCategory WHERE categoryId=?",
            params![category_id],
            |row| row.get::<_, i64>(0),
        )?;
        if count == 0 {
            transaction.execute("DELETE FROM gifts WHERE categoryId=?", params![category_id])?;
            transaction.execute("DELETE FROM categories WHERE id=?", params![category_id])?;
        }
        transaction.commit()?;

        Ok(())
    }

    pub fn add_gift(
        &self,
        name: &str,
        description: Option<String>,
        price: Option<String>,
        where_to_buy: Option<String>,
        picture: Option<String>,
        secret: bool,
        category_id: i64,
    ) -> Result<i64, WishlistManagerError> {
        let connection = self.connection.lock().unwrap();

        let sql = if secret {
            "SELECT MAX(rank) FROM gifts WHERE categoryId=?"
        } else {
            "SELECT MAX(rank) FROM gifts WHERE categoryId=? AND rank < 100000"
        };

        let mut rank = connection
            .query_row(sql, params![category_id], |row| {
                row.get::<_, Option<i64>>(0)
            })?
            .unwrap_or(-1);

        if secret && rank < 100000 {
            rank = 100000;
        }

        connection.execute("INSERT INTO gifts (name, description, price, whereToBuy, picture, rank, secret, heart, categoryId) VALUES (?,?,?,?,?,?,?,FALSE,?)", params![name, description, price, where_to_buy, picture, rank+1, secret, category_id])?;
        let gift_id = connection.last_insert_rowid();
        Ok(gift_id)
    }

    pub fn edit_gift(
        &self,
        gift_id: i64,
        name: &str,
        description: Option<String>,
        price: Option<String>,
        where_to_buy: Option<String>,
        picture: Option<String>,
        category_id: i64,
    ) -> Result<(), WishlistManagerError> {
        let connection = self.connection.lock().unwrap();
        connection.execute("UPDATE gifts SET name=?, description=?, price=?, whereToBuy=?, picture=?, categoryId=? WHERE id=?", params![name, description, price, where_to_buy, picture, category_id, gift_id])?;

        Ok(())
    }

    pub fn reorder_gifts(
        &self,
        starting_rank: usize,
        gifts: &[i64],
    ) -> Result<(), WishlistManagerError> {
        let mut connection = self.connection.lock().unwrap();
        let transaction = connection.transaction()?;
        for (index, gift) in gifts.iter().enumerate() {
            transaction.execute(
                "UPDATE gifts SET rank=? WHERE id=?",
                params![index + starting_rank, gift],
            )?;
        }
        transaction.commit()?;

        Ok(())
    }

    pub fn delete_gift(&self, gift_id: i64) -> Result<(), WishlistManagerError> {
        let connection = self.connection.lock().unwrap();
        connection.execute("DELETE FROM gifts WHERE id=?", params![gift_id])?;
        Ok(())
    }

    pub fn change_heart_gift(&self, gift_id: i64) -> Result<(), WishlistManagerError> {
        let connection = self.connection.lock().unwrap();

        connection.execute(
            "UPDATE gifts SET heart=NOT heart WHERE id=?",
            params![gift_id],
        )?;

        Ok(())
    }

    pub fn reserve_gift(
        &self,
        gift_id: i64,
        user_id: Option<i64>,
    ) -> Result<(), WishlistManagerError> {
        let connection = self.connection.lock().unwrap();

        connection.execute(
            "UPDATE gifts SET reservedBy=? WHERE id=?",
            params![user_id, gift_id],
        )?;

        Ok(())
    }

    pub fn is_my_category(
        &self,
        user_id: i64,
        category_id: i64,
    ) -> Result<bool, WishlistManagerError> {
        let connection = self.connection.lock().unwrap();

        let mut statement = connection
            .prepare("SELECT userId FROM joinUserAndCategory WHERE userId=? and categoryId=?")?;
        Ok(statement.exists(params![user_id, category_id])?)
    }

    pub fn is_my_gift(
        &self,
        user_id: i64,
        category_id: i64,
        gift_id: i64,
    ) -> Result<bool, WishlistManagerError> {
        {
            let connection = self.connection.lock().unwrap();
            let mut statement =
                connection.prepare("SELECT categoryId FROM gifts WHERE id=? AND categoryId=?")?;
            if !statement.exists(params![gift_id, category_id])? {
                return Ok(false);
            }
        }

        self.is_my_category(user_id, category_id)
    }

    pub fn is_my_gift_for_edit(
        &self,
        user_id: i64,
        gift_id: i64,
    ) -> Result<bool, WishlistManagerError> {
        let old_category = {
            let connection = self.connection.lock().unwrap();
            let Some(old_category) = connection
                .query_row(
                    "SELECT categoryId FROM gifts WHERE id=?",
                    params![gift_id],
                    |row| row.get::<_, i64>(0),
                )
                .optional()?
            else {
                return Ok(false);
            };
            old_category
        };

        self.is_my_category(user_id, old_category)
    }

    pub fn is_gift_reserved(&self, gift_id: i64) -> Result<bool, WishlistManagerError> {
        let connection = self.connection.lock().unwrap();
        let mut statement =
            connection.prepare("SELECT id FROM gifts WHERE id=? AND reservedBy!=NULL")?;
        Ok(statement.exists(params![gift_id])?)
    }

    pub fn is_gift_reserved_by_me(
        &self,
        gift_id: i64,
        user_id: i64,
    ) -> Result<bool, WishlistManagerError> {
        let connection = self.connection.lock().unwrap();
        let mut statement =
            connection.prepare("SELECT id FROM gifts WHERE id=? AND reservedBy=?")?;
        Ok(statement.exists(params![gift_id, user_id])?)
    }

    /**
    This return our wishlist with the shared categories but without secret gifts
    **/
    pub fn get_my_wishlist(&self, user_id: i64) -> Result<WishList, WishlistManagerError> {
        let connection = self.connection.lock().unwrap();

        let mut statement = connection.prepare("SELECT id, name FROM joinUserAndCategory j LEFT JOIN categories c ON c.id=j.categoryId WHERE j.userId=? ORDER BY j.rank")?;
        let rows = statement.query_map(params![user_id], |row| <_>::try_from(row))?;

        let mut categories = Vec::new();
        for row in rows {
            let mut category: Category = row?;
            let mut statement = connection.prepare(
                "SELECT userId FROM joinUserAndCategory WHERE categoryId=? AND userId!=?",
            )?;
            let rows = statement.query_map(params![category.id, user_id], |row| row.get(0))?;
            for row in rows {
                category.share_with.push(row?)
            }
            let mut statement = connection.prepare("SELECT id, name, description, price, whereToBuy, picture, heart FROM gifts WHERE categoryId=? AND secret=FALSE ORDER BY rank")?;
            let rows = statement.query_map(params![category.id], |row| <_>::try_from(row))?;
            for row in rows {
                category.gifts.push(row?)
            }
            categories.push(category);
        }
        Ok(WishList { categories })
    }

    /**
    This return friend wishlist without the shared categories but with secret gifts
    **/
    pub fn get_friend_wishlist(
        &self,
        user_id: i64,
        friend_id: i64,
    ) -> Result<FriendWishList, WishlistManagerError> {
        let connection = self.connection.lock().unwrap();

        let mut statement = connection.prepare("SELECT id, name FROM joinUserAndCategory j LEFT JOIN categories c ON c.id=j.categoryId WHERE j.userId=? AND \
            j.categoryId NOT IN (SELECT categoryId FROM joinUserAndCategory WHERE userId=?) ORDER BY j.rank")?;
        let rows = statement.query_map(params![friend_id, user_id], |row| <_>::try_from(row))?;

        let mut categories = Vec::new();
        for row in rows {
            let mut category: FriendCategory = row?;
            let mut statement = connection.prepare("SELECT id, name, description, price, whereToBuy, picture, heart, secret, reservedBy FROM gifts WHERE categoryId=? ORDER BY rank")?;
            let rows = statement.query_map(params![category.id], |row| <_>::try_from(row))?;
            for row in rows {
                category.gifts.push(row?)
            }
            categories.push(category);
        }
        Ok(FriendWishList { categories })
    }
}

#[derive(Serialize)]
#[cfg_attr(test, derive(PartialEq, Eq, Debug))]
pub struct WishList {
    pub categories: Vec<Category>,
}
#[derive(Serialize)]
#[cfg_attr(test, derive(PartialEq, Eq, Debug))]
pub struct Category {
    id: i64,
    pub name: String,
    share_with: Vec<i64>,
    pub gifts: Vec<Gift>,
}
#[derive(Serialize)]
#[cfg_attr(test, derive(PartialEq, Eq, Debug))]
pub struct Gift {
    id: i64,
    pub name: String,
    pub description: Option<String>,
    pub price: Option<String>,
    pub where_to_buy: Option<String>,
    pub picture: Option<String>,
    pub heart: bool,
}
impl<'a> TryFrom<&Row<'a>> for Category {
    type Error = rusqlite::Error;

    fn try_from(row: &Row<'a>) -> Result<Self, Self::Error> {
        Ok(Self {
            id: row.get(0)?,
            name: row.get(1)?,
            share_with: Vec::new(),
            gifts: Vec::new(),
        })
    }
}
impl<'a> TryFrom<&Row<'a>> for Gift {
    type Error = rusqlite::Error;

    fn try_from(row: &Row<'a>) -> Result<Self, Self::Error> {
        Ok(Self {
            id: row.get(0)?,
            name: row.get(1)?,
            description: row.get(2)?,
            price: row.get(3)?,
            where_to_buy: row.get(4)?,
            picture: row.get(5)?,
            heart: row.get(6)?,
        })
    }
}

#[derive(Serialize)]
#[cfg_attr(test, derive(PartialEq, Eq, Debug))]
pub struct FriendWishList {
    categories: Vec<FriendCategory>,
}
#[derive(Serialize)]
#[cfg_attr(test, derive(PartialEq, Eq, Debug))]
pub struct FriendCategory {
    id: i64,
    name: String,
    gifts: Vec<FriendGift>,
}
#[derive(Serialize)]
#[cfg_attr(test, derive(PartialEq, Eq, Debug))]
pub struct FriendGift {
    id: i64,
    name: String,
    description: Option<String>,
    price: Option<String>,
    where_to_buy: Option<String>,
    picture: Option<String>,
    heart: bool,
    secret: bool,
    reserved_by: Option<i64>,
}
impl<'a> TryFrom<&Row<'a>> for FriendCategory {
    type Error = rusqlite::Error;

    fn try_from(row: &Row<'a>) -> Result<Self, Self::Error> {
        Ok(Self {
            id: row.get(0)?,
            name: row.get(1)?,
            gifts: Vec::new(),
        })
    }
}
impl<'a> TryFrom<&Row<'a>> for FriendGift {
    type Error = rusqlite::Error;

    fn try_from(row: &Row<'a>) -> Result<Self, Self::Error> {
        Ok(Self {
            id: row.get(0)?,
            name: row.get(1)?,
            description: row.get(2)?,
            price: row.get(3)?,
            where_to_buy: row.get(4)?,
            picture: row.get(5)?,
            heart: row.get(6)?,
            secret: row.get(7)?,
            reserved_by: row.get(8)?,
        })
    }
}
impl Into<WishList> for FriendWishList {
    fn into(self) -> WishList {
        WishList {
            categories: self
                .categories
                .into_iter()
                .map(|c| Category {
                    id: c.id,
                    name: c.name,
                    share_with: Vec::new(),
                    gifts: c
                        .gifts
                        .into_iter()
                        .filter(|g| g.reserved_by.is_none())
                        .map(|g| Gift {
                            id: g.id,
                            name: g.name,
                            description: g.description,
                            price: g.price,
                            where_to_buy: g.where_to_buy,
                            picture: g.picture,
                            heart: g.heart,
                        })
                        .collect(),
                })
                .collect(),
        }
    }
}

#[cfg(test)]
mod test {
    use crate::managers::test_helper::create_test_database;
    use crate::managers::users_manager::UsersManager;
    use crate::managers::wishlist_manager::{Category, Gift, WishList, WishlistManager};
    use std::collections::HashSet;
    use std::sync::{Arc, Mutex};

    #[test]
    fn test_add_category() {
        let connection = Arc::new(Mutex::new(create_test_database("test_add_category")));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();
        let two = users_manager.add_user("two", "pwd").unwrap();

        let wishlist_manager = WishlistManager::new(connection).unwrap();
        wishlist_manager
            .add_category("OneCategory", HashSet::from([one]))
            .unwrap();
        wishlist_manager
            .add_category("TwoCategory", HashSet::from([two]))
            .unwrap();
        wishlist_manager
            .add_category("TwoCategory2", HashSet::from([two]))
            .unwrap();
        wishlist_manager
            .add_category("SharedCategory", HashSet::from([one, two]))
            .unwrap();

        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![
                    Category {
                        id: 1,
                        name: "OneCategory".to_string(),
                        share_with: Vec::new(),
                        gifts: Vec::new()
                    },
                    Category {
                        id: 4,
                        name: "SharedCategory".to_string(),
                        share_with: vec![two],
                        gifts: Vec::new()
                    }
                ]
            }
        );
        let wishlist = wishlist_manager.get_my_wishlist(two).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![
                    Category {
                        id: 2,
                        name: "TwoCategory".to_string(),
                        share_with: Vec::new(),
                        gifts: Vec::new()
                    },
                    Category {
                        id: 3,
                        name: "TwoCategory2".to_string(),
                        share_with: Vec::new(),
                        gifts: Vec::new()
                    },
                    Category {
                        id: 4,
                        name: "SharedCategory".to_string(),
                        share_with: vec![one],
                        gifts: Vec::new()
                    }
                ]
            }
        );
    }

    #[test]
    fn test_edit_category() {
        let connection = Arc::new(Mutex::new(create_test_database("test_edit_category")));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();
        let two = users_manager.add_user("two", "pwd").unwrap();
        let three = users_manager.add_user("three", "pwd").unwrap();

        let wishlist_manager = WishlistManager::new(connection).unwrap();
        wishlist_manager
            .add_category("SharedCategory", HashSet::from([one, two]))
            .unwrap();

        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "SharedCategory".to_string(),
                    share_with: vec![two],
                    gifts: Vec::new()
                }]
            }
        );
        let wishlist = wishlist_manager.get_my_wishlist(two).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "SharedCategory".to_string(),
                    share_with: vec![one],
                    gifts: Vec::new()
                }]
            }
        );
        let wishlist = wishlist_manager.get_my_wishlist(three).unwrap();
        assert_eq!(wishlist, WishList { categories: vec![] });

        wishlist_manager
            .edit_category(1, "NewName", HashSet::from([one, three]))
            .unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "NewName".to_string(),
                    share_with: vec![three],
                    gifts: Vec::new()
                }]
            }
        );
        let wishlist = wishlist_manager.get_my_wishlist(two).unwrap();
        assert_eq!(wishlist, WishList { categories: vec![] });
        let wishlist = wishlist_manager.get_my_wishlist(three).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "NewName".to_string(),
                    share_with: vec![one],
                    gifts: Vec::new()
                }]
            }
        );
    }

    #[test]
    fn test_reorder_categories() {
        let connection = Arc::new(Mutex::new(create_test_database("test_reorder_categories")));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();
        let two = users_manager.add_user("two", "pwd").unwrap();

        let wishlist_manager = WishlistManager::new(connection).unwrap();
        wishlist_manager
            .add_category("OneCategory", HashSet::from([one]))
            .unwrap();
        wishlist_manager
            .add_category("TwoCategory", HashSet::from([two]))
            .unwrap();
        wishlist_manager
            .add_category("TwoCategory2", HashSet::from([two]))
            .unwrap();
        wishlist_manager
            .add_category("SharedCategory", HashSet::from([one, two]))
            .unwrap();

        //Reorder first and second of user two
        wishlist_manager
            .reorder_categories(two, 0, &[3, 2])
            .unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(two).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![
                    Category {
                        id: 3,
                        name: "TwoCategory2".to_string(),
                        share_with: Vec::new(),
                        gifts: Vec::new()
                    },
                    Category {
                        id: 2,
                        name: "TwoCategory".to_string(),
                        share_with: Vec::new(),
                        gifts: Vec::new()
                    },
                    Category {
                        id: 4,
                        name: "SharedCategory".to_string(),
                        share_with: vec![one],
                        gifts: Vec::new()
                    }
                ]
            }
        );

        //Reorder share does not affect user one
        wishlist_manager
            .reorder_categories(two, 1, &[4, 2])
            .unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(two).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![
                    Category {
                        id: 3,
                        name: "TwoCategory2".to_string(),
                        share_with: Vec::new(),
                        gifts: Vec::new()
                    },
                    Category {
                        id: 4,
                        name: "SharedCategory".to_string(),
                        share_with: vec![one],
                        gifts: Vec::new()
                    },
                    Category {
                        id: 2,
                        name: "TwoCategory".to_string(),
                        share_with: Vec::new(),
                        gifts: Vec::new()
                    }
                ]
            }
        );
        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![
                    Category {
                        id: 1,
                        name: "OneCategory".to_string(),
                        share_with: Vec::new(),
                        gifts: Vec::new()
                    },
                    Category {
                        id: 4,
                        name: "SharedCategory".to_string(),
                        share_with: vec![two],
                        gifts: Vec::new()
                    }
                ]
            }
        );
    }

    #[test]
    fn test_add_gift() {
        let connection = Arc::new(Mutex::new(create_test_database("test_add_gift")));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();
        let two = users_manager.add_user("two", "pwd").unwrap();

        let wishlist_manager = WishlistManager::new(connection).unwrap();
        wishlist_manager
            .add_category("OneCategory", HashSet::from([one]))
            .unwrap();
        wishlist_manager
            .add_category("SharedCategory", HashSet::from([one, two]))
            .unwrap();

        wishlist_manager
            .add_gift("Gift", None, None, None, None, false, 1)
            .unwrap();
        wishlist_manager
            .add_gift(
                "Gift2",
                Some("desc".to_string()),
                Some("price".to_string()),
                Some("wtb".to_string()),
                Some("pic".to_string()),
                false,
                2,
            )
            .unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![
                    Category {
                        id: 1,
                        name: "OneCategory".to_string(),
                        share_with: Vec::new(),
                        gifts: vec![Gift {
                            id: 1,
                            name: "Gift".to_string(),
                            description: None,
                            price: None,
                            where_to_buy: None,
                            picture: None,
                            heart: false
                        }]
                    },
                    Category {
                        id: 2,
                        name: "SharedCategory".to_string(),
                        share_with: vec![two],
                        gifts: vec![Gift {
                            id: 2,
                            name: "Gift2".to_string(),
                            description: Some("desc".to_string()),
                            price: Some("price".to_string()),
                            where_to_buy: Some("wtb".to_string()),
                            picture: Some("pic".to_string()),
                            heart: false
                        }]
                    }
                ]
            }
        );
        let wishlist = wishlist_manager.get_my_wishlist(two).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 2,
                    name: "SharedCategory".to_string(),
                    share_with: vec![one],
                    gifts: vec![Gift {
                        id: 2,
                        name: "Gift2".to_string(),
                        description: Some("desc".to_string()),
                        price: Some("price".to_string()),
                        where_to_buy: Some("wtb".to_string()),
                        picture: Some("pic".to_string()),
                        heart: false
                    }]
                }]
            }
        );
    }

    #[test]
    fn test_edit_gift() {
        let connection = Arc::new(Mutex::new(create_test_database("test_edit_gift")));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();

        let wishlist_manager = WishlistManager::new(connection).unwrap();
        wishlist_manager
            .add_category("OneCategory", HashSet::from([one]))
            .unwrap();

        wishlist_manager
            .add_gift("Gift", None, None, None, None, false, 1)
            .unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "OneCategory".to_string(),
                    share_with: Vec::new(),
                    gifts: vec![Gift {
                        id: 1,
                        name: "Gift".to_string(),
                        description: None,
                        price: None,
                        where_to_buy: None,
                        picture: None,
                        heart: false
                    }]
                }]
            }
        );

        wishlist_manager
            .edit_gift(
                1,
                "NewName",
                Some("desc".to_string()),
                Some("price".to_string()),
                Some("wtb".to_string()),
                Some("pic".to_string()),
                1,
            )
            .unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "OneCategory".to_string(),
                    share_with: Vec::new(),
                    gifts: vec![Gift {
                        id: 1,
                        name: "NewName".to_string(),
                        description: Some("desc".to_string()),
                        price: Some("price".to_string()),
                        where_to_buy: Some("wtb".to_string()),
                        picture: Some("pic".to_string()),
                        heart: false
                    }]
                }]
            }
        );
    }

    #[test]
    fn test_reorder_gifts() {
        let connection = Arc::new(Mutex::new(create_test_database("test_reorder_gifts")));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();

        let wishlist_manager = WishlistManager::new(connection).unwrap();
        wishlist_manager
            .add_category("OneCategory", HashSet::from([one]))
            .unwrap();

        wishlist_manager
            .add_gift("Gift", None, None, None, None, false, 1)
            .unwrap();
        wishlist_manager
            .add_gift("Secret", None, None, None, None, true, 1)
            .unwrap();
        wishlist_manager
            .add_gift("Gift2", None, None, None, None, false, 1)
            .unwrap();

        //Reorder from the user that can only see gift 1 and 3
        wishlist_manager.reorder_gifts(0, &[3, 1]).unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "OneCategory".to_string(),
                    share_with: Vec::new(),
                    gifts: vec![
                        Gift {
                            id: 3,
                            name: "Gift2".to_string(),
                            description: None,
                            price: None,
                            where_to_buy: None,
                            picture: None,
                            heart: false
                        },
                        Gift {
                            id: 1,
                            name: "Gift".to_string(),
                            description: None,
                            price: None,
                            where_to_buy: None,
                            picture: None,
                            heart: false
                        }
                    ]
                }]
            }
        );
    }

    #[test]
    fn test_change_heart() {
        let connection = Arc::new(Mutex::new(create_test_database("test_change_heart")));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();

        let wishlist_manager = WishlistManager::new(connection).unwrap();
        wishlist_manager
            .add_category("OneCategory", HashSet::from([one]))
            .unwrap();

        wishlist_manager
            .add_gift("Gift", None, None, None, None, false, 1)
            .unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "OneCategory".to_string(),
                    share_with: Vec::new(),
                    gifts: vec![Gift {
                        id: 1,
                        name: "Gift".to_string(),
                        description: None,
                        price: None,
                        where_to_buy: None,
                        picture: None,
                        heart: false
                    }]
                }]
            }
        );

        wishlist_manager.change_heart_gift(1).unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "OneCategory".to_string(),
                    share_with: Vec::new(),
                    gifts: vec![Gift {
                        id: 1,
                        name: "Gift".to_string(),
                        description: None,
                        price: None,
                        where_to_buy: None,
                        picture: None,
                        heart: true
                    }]
                }]
            }
        );

        wishlist_manager.change_heart_gift(1).unwrap();
        let wishlist = wishlist_manager.get_my_wishlist(one).unwrap();
        assert_eq!(
            wishlist,
            WishList {
                categories: vec![Category {
                    id: 1,
                    name: "OneCategory".to_string(),
                    share_with: Vec::new(),
                    gifts: vec![Gift {
                        id: 1,
                        name: "Gift".to_string(),
                        description: None,
                        price: None,
                        where_to_buy: None,
                        picture: None,
                        heart: false
                    }]
                }]
            }
        );
    }
}
