use crate::managers::friends_manager::{FriendsManager, FriendsManagerError};
use chrono::{DateTime, Utc};
use rusqlite::{params, Connection, Row};
use std::sync::{Arc, Mutex};
use std::time::SystemTime;
use serde::Serialize;

#[derive(Clone)]
pub struct NotificationsManager {
    connection: Arc<Mutex<Connection>>,
    friends_manager: FriendsManager,
}

#[derive(thiserror::Error, Debug)]
#[error(transparent)]
pub enum NotificationsManagerError {
    Sqlite(#[from] rusqlite::Error),
    FriendsManagerError(#[from] FriendsManagerError),
}

pub enum NotificationEvent {
    CreateCategory(i64),
    UpdateCategory(i64),
    DeleteCategory(i64),
    CreateGift(i64),
    UpdateGift(i64),
    DeleteGift(i64),
}

impl NotificationEvent {
    fn to_sql_type(&self) -> &'static str {
        match self {
            NotificationEvent::CreateCategory(_) => "CreateCategory",
            NotificationEvent::UpdateCategory(_) => "UpdateCategory",
            NotificationEvent::DeleteCategory(_) => "DeleteCategory",
            NotificationEvent::CreateGift(_) => "CreateGift",
            NotificationEvent::UpdateGift(_) => "UpdateGift",
            NotificationEvent::DeleteGift(_) => "DeleteGift",
        }
    }

    fn to_sql_detail(&self) -> String {
        match self {
            NotificationEvent::CreateCategory(id) => id.to_string(),
            NotificationEvent::UpdateCategory(id) => id.to_string(),
            NotificationEvent::DeleteCategory(id) => id.to_string(),
            NotificationEvent::CreateGift(id) => id.to_string(),
            NotificationEvent::UpdateGift(id) => id.to_string(),
            NotificationEvent::DeleteGift(id) => id.to_string(),
        }
    }
}

#[derive(Serialize)]
#[cfg_attr(test, derive(Debug))]
pub struct Notification {
    id: i64,
    #[serde(rename="type")]
    _type: String,
    detail: String,
    created_at: i64,
    created_by: String,
    read: bool,
}

#[cfg(test)]
impl PartialEq for Notification {
    fn eq(&self, other: &Self) -> bool {
        self._type == other._type
            && self.detail == other.detail
            && self.created_by == other.created_by
            && self.read == other.read
    }
}

impl<'a> TryFrom<&Row<'a>> for Notification {
    type Error = rusqlite::Error;

    fn try_from(row: &Row<'a>) -> Result<Self, Self::Error> {
        Ok(Self {
            id: row.get(0)?,
            _type: row.get(1)?,
            detail: row.get(2)?,
            created_at: row.get(3)?,
            created_by: row.get(4)?,
            read: row.get(5)?,
        })
    }
}

impl NotificationsManager {
    pub fn new(
        connection: Arc<Mutex<Connection>>,
        friends_manager: FriendsManager,
    ) -> Result<Self, NotificationsManagerError> {
        Self::init_database(&connection)?;
        Ok(Self {
            connection,
            friends_manager,
        })
    }

    fn init_database(connection: &Arc<Mutex<Connection>>) -> Result<(), NotificationsManagerError> {
        let connection = connection.lock().unwrap();
        connection.execute_batch(
            "CREATE TABLE IF NOT EXISTS notifications_details (\
            id INTEGER PRIMARY KEY AUTOINCREMENT, \
            type TEXT NOT NULL, \
            detail TEXT NOT NULL,\
            created_at LONG NOT NULL,\
            created_by INTEGER NOT NULL, \
            FOREIGN KEY(created_by) REFERENCES users(id));\
        CREATE TABLE IF NOT EXISTS notifications (\
            id INTEGER PRIMARY KEY AUTOINCREMENT,\
            notification_detail INTEGER NOT NULL,\
            recipient INTEGER NOT NULL,\
            read INTEGER NOT NULL DEFAULT FALSE,\
            FOREIGN KEY(recipient) REFERENCES users(id),\
            FOREIGN KEY(notification_detail) REFERENCES notifications_details(id));",
        )?;
        Ok(())
    }

    pub fn add_notification(
        &self,
        user_id: i64,
        notification_event: NotificationEvent,
    ) -> Result<(), NotificationsManagerError> {
        let friends = self.friends_manager.get_friends(user_id)?;
        if friends.is_empty() {
            return Ok(());
        }

        let mut connection = self.connection.lock().unwrap();
        let transaction = connection.transaction()?;
        let now: DateTime<Utc> = SystemTime::now().into();
        transaction.execute("INSERT INTO notifications_details(type, detail, created_at, created_by) VALUES (?,?,?,?)", 
                            params![notification_event.to_sql_type(), notification_event.to_sql_detail(), now.timestamp(), user_id])?;

        let notification_detail_id = transaction.last_insert_rowid();
        for friend in friends {
            transaction.execute(
                "INSERT INTO notifications (notification_detail, recipient) VALUES (?,?)",
                params![notification_detail_id, friend.id],
            )?;
        }

        Ok(transaction.commit()?)
    }

    pub fn get_notifications(
        &self,
        user_id: i64,
    ) -> Result<Vec<Notification>, NotificationsManagerError> {
        let connection = self.connection.lock().unwrap();
        let mut statement = connection.prepare_cached(
            "SELECT N.id, type, detail, created_at, U.name, read from notifications N \
            LEFT JOIN notifications_details ND ON N.notification_detail = ND.id \
            LEFT JOIN users U ON ND.created_by = U.id \
            WHERE recipient=?",
        )?;
        let rows = statement.query_map(params![user_id], |row| <_>::try_from(row))?;
        let mut notifications = Vec::new();
        for row in rows {
            notifications.push(row?);
        }
        Ok(notifications)
    }

    pub fn read_notification(&self, notification_id: i64) -> Result<(), NotificationsManagerError> {
        let connection = self.connection.lock().unwrap();
        connection.execute(
            "UPDATE notifications SET read=TRUE WHERE id=?",
            params![notification_id],
        )?;
        Ok(())
    }
}

#[cfg(test)]
mod test {
    use crate::managers::friends_manager::{FriendsManager, RequestStatus};
    use crate::managers::notifications_manager::{
        Notification, NotificationEvent, NotificationsManager,
    };
    use crate::managers::test_helper::create_test_database;
    use crate::managers::users_manager::UsersManager;
    use rusqlite::params;
    use std::sync::{Arc, Mutex};

    #[test]
    fn test_add_notification() {
        let connection = Arc::new(Mutex::new(create_test_database("test_add_notification")));

        //Make two users friend
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let user_one = users_manager.add_user("one", "pwd").unwrap();
        let user_two = users_manager.add_user("two", "pwd").unwrap();
        let user_three = users_manager.add_user("three", "pwd").unwrap();
        let friends_manager = FriendsManager::new(connection.clone()).unwrap();
        friends_manager
            .create_friend_request(user_one, "two")
            .unwrap();
        friends_manager
            .create_friend_request(user_one, "three")
            .unwrap();
        let requests = friends_manager.get_requests(user_two).unwrap();
        for request in requests.received {
            friends_manager
                .update_received_request(request.id, user_two, RequestStatus::Accepted)
                .unwrap()
        }
        let requests = friends_manager.get_requests(user_three).unwrap();
        for request in requests.received {
            friends_manager
                .update_received_request(request.id, user_three, RequestStatus::Accepted)
                .unwrap()
        }

        let notifications_manager = NotificationsManager::new(connection, friends_manager).unwrap();
        notifications_manager
            .add_notification(user_one, NotificationEvent::CreateGift(42))
            .unwrap();

        let notifications = notifications_manager.get_notifications(user_one).unwrap();
        assert!(notifications.is_empty());
        let notifications = notifications_manager.get_notifications(user_two).unwrap();
        assert_eq!(
            notifications,
            vec![Notification {
                id: 0,
                _type: "CreateGift".to_string(),
                detail: "42".to_string(),
                created_at: 0,
                created_by: "one".to_string(),
                read: false
            }]
        );
        let notifications = notifications_manager.get_notifications(user_three).unwrap();
        assert_eq!(
            notifications,
            vec![Notification {
                id: 0,
                _type: "CreateGift".to_string(),
                detail: "42".to_string(),
                created_at: 0,
                created_by: "one".to_string(),
                read: false
            }]
        );
    }

    #[test]
    fn test_add_notification_no_friend() {
        let connection = Arc::new(Mutex::new(create_test_database(
            "test_add_notification_no_friend",
        )));

        //Make two users friend
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let user_one = users_manager.add_user("one", "pwd").unwrap();
        let friends_manager = FriendsManager::new(connection.clone()).unwrap();

        let notifications_manager =
            NotificationsManager::new(connection.clone(), friends_manager).unwrap();
        notifications_manager
            .add_notification(user_one, NotificationEvent::CreateGift(1))
            .unwrap();

        let connection = connection.lock().unwrap();
        assert_eq!(
            connection
                .query_row("SELECT count(*) from notifications", params![], |row| row
                    .get::<_, i64>(
                    0
                ))
                .unwrap(),
            0
        );
        assert_eq!(
            connection
                .query_row(
                    "SELECT count(*) from notifications_details",
                    params![],
                    |row| row.get::<_, i64>(0)
                )
                .unwrap(),
            0
        );
    }

    #[test]
    fn test_read_notification() {
        let connection = Arc::new(Mutex::new(create_test_database("test_read_notification")));

        //Make two users friend
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let user_one = users_manager.add_user("one", "pwd").unwrap();
        let user_two = users_manager.add_user("two", "pwd").unwrap();
        let user_three = users_manager.add_user("three", "pwd").unwrap();
        let friends_manager = FriendsManager::new(connection.clone()).unwrap();
        friends_manager
            .create_friend_request(user_one, "two")
            .unwrap();
        friends_manager
            .create_friend_request(user_one, "three")
            .unwrap();
        let requests = friends_manager.get_requests(user_two).unwrap();
        for request in requests.received {
            friends_manager
                .update_received_request(request.id, user_two, RequestStatus::Accepted)
                .unwrap()
        }
        let requests = friends_manager.get_requests(user_three).unwrap();
        for request in requests.received {
            friends_manager
                .update_received_request(request.id, user_three, RequestStatus::Accepted)
                .unwrap()
        }

        let notifications_manager = NotificationsManager::new(connection, friends_manager).unwrap();
        notifications_manager
            .add_notification(user_one, NotificationEvent::CreateGift(42))
            .unwrap();

        //read user two
        let notifications = notifications_manager.get_notifications(user_two).unwrap();
        notifications_manager
            .read_notification(notifications.first().unwrap().id)
            .unwrap();
        let notifications = notifications_manager.get_notifications(user_two).unwrap();
        assert_eq!(
            notifications,
            vec![Notification {
                id: 0,
                _type: "CreateGift".to_string(),
                detail: "42".to_string(),
                created_at: 0,
                created_by: "one".to_string(),
                read: true
            }]
        );
        let notifications = notifications_manager.get_notifications(user_three).unwrap();
        assert_eq!(
            notifications,
            vec![Notification {
                id: 0,
                _type: "CreateGift".to_string(),
                detail: "42".to_string(),
                created_at: 0,
                created_by: "one".to_string(),
                read: false
            }]
        );

        //read user three
        let notifications = notifications_manager.get_notifications(user_three).unwrap();
        notifications_manager
            .read_notification(notifications.first().unwrap().id)
            .unwrap();
        let notifications = notifications_manager.get_notifications(user_two).unwrap();
        assert_eq!(
            notifications,
            vec![Notification {
                id: 0,
                _type: "CreateGift".to_string(),
                detail: "42".to_string(),
                created_at: 0,
                created_by: "one".to_string(),
                read: true
            }]
        );
        let notifications = notifications_manager.get_notifications(user_three).unwrap();
        assert_eq!(
            notifications,
            vec![Notification {
                id: 0,
                _type: "CreateGift".to_string(),
                detail: "42".to_string(),
                created_at: 0,
                created_by: "one".to_string(),
                read: true
            }]
        );
    }
}
