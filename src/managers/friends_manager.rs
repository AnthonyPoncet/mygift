use crate::managers::users_manager::{CleanUser, UsersManager, UsersManagerError};
use rusqlite::types::ToSqlOutput;
use rusqlite::{params, Connection, ToSql};
use serde::Serialize;
use std::sync::{Arc, Mutex};

#[derive(Clone)]
pub struct FriendsManager {
    connection: Arc<Mutex<Connection>>,
}

#[derive(thiserror::Error, Debug)]
#[error(transparent)]
pub(crate) enum FriendsManagerError {
    Sqlite(#[from] rusqlite::Error),
    #[error("Unknown user {0}")]
    UnknownUser(String),
    #[error("{0} tried to ask himself as a friend")]
    CannotAskYourself(i64),
    #[error("Friend request between {0} and {1} already exists")]
    FriendRequestAlreadyExists(i64, i64),
    #[error("Friend request with id {0} for user {1} does not exist")]
    FriendRequestDoesNotExists(i64, i64),
}

impl From<UsersManagerError> for FriendsManagerError {
    fn from(value: UsersManagerError) -> Self {
        match value {
            UsersManagerError::Sqlite(e) => FriendsManagerError::Sqlite(e),
            UsersManagerError::UnknownUser(u) => FriendsManagerError::UnknownUser(u),
            UsersManagerError::UserAlreadyExist(_) | UsersManagerError::PasswordMismatch => {
                panic!("Should never happen")
            }
        }
    }
}

impl FriendsManager {
    pub fn new(connection: Arc<Mutex<Connection>>) -> Result<Self, FriendsManagerError> {
        Self::init_database(&connection)?;
        Ok(Self { connection })
    }

    fn init_database(connection: &Arc<Mutex<Connection>>) -> Result<(), FriendsManagerError> {
        let connection = connection.lock().unwrap();
        connection.execute_batch("CREATE TABLE IF NOT EXISTS friendRequests (id INTEGER PRIMARY KEY AUTOINCREMENT, userOne INTEGER NOT NULL, userTwo INTEGER NOT NULL,
        status TEXT NOT NULL, FOREIGN KEY(userOne) REFERENCES users(id), FOREIGN KEY(userTwo) REFERENCES users(id))")?;
        Ok(())
    }

    pub fn create_friend_request(
        &self,
        from_user_id: i64,
        to_user: &str,
    ) -> Result<(), FriendsManagerError> {
        let connection = self.connection.lock().unwrap();
        let to_user = UsersManager::static_get_user(&connection, to_user)?;
        if to_user.id == from_user_id {
            return Err(FriendsManagerError::CannotAskYourself(from_user_id));
        }

        let mut statement = connection.prepare("SELECT 1 FROM friendRequests WHERE userOne=? AND userTwo=? UNION ALL SELECT id FROM friendRequests WHERE userOne=? AND userTwo=?")?;
        if statement.exists(params![from_user_id, to_user.id, to_user.id, from_user_id])? {
            return Err(FriendsManagerError::FriendRequestAlreadyExists(
                from_user_id,
                to_user.id,
            ));
        }

        connection.execute(
            "INSERT INTO friendRequests(userOne,userTwo,status) VALUES (?, ?, ?)",
            params![from_user_id, to_user.id, "PENDING"],
        )?;
        Ok(())
    }

    pub fn get_friends(&self, user_id: i64) -> Result<Vec<CleanUser>, FriendsManagerError> {
        let connection = self.connection.lock().unwrap();

        let mut statement = connection.prepare(
            "WITH friends AS (SELECT userTwo as user_id FROM friendRequests WHERE userOne=? and status=?
            UNION ALL 
            SELECT userOne as user_id FROM friendRequests WHERE userTwo=? and status=?)
            SELECT id, name, picture, dateOfBirth FROM users WHERE id IN (SELECT user_id FROM friends)")?;

        let rows = statement.query_map(
            params![
                user_id,
                RequestStatus::Accepted,
                user_id,
                RequestStatus::Accepted
            ],
            |row| CleanUser::try_from(row),
        )?;

        let mut friends = Vec::new();
        for row in rows {
            friends.push(row?);
        }

        Ok(friends)
    }

    pub fn get_friend_id(
        &self,
        user_id: i64,
        friend_name: &str,
    ) -> Result<i64, FriendsManagerError> {
        let friend = {
            let connection = self.connection.lock().unwrap();
            UsersManager::static_get_user(&connection, friend_name)?
        };

        if !self.is_my_friend(user_id, friend.id)? {
            return Err(FriendsManagerError::UnknownUser(friend_name.to_string()));
        }

        Ok(friend.id)
    }

    pub fn get_requests(&self, user_id: i64) -> Result<Requests, FriendsManagerError> {
        let connection = self.connection.lock().unwrap();

        let sent = Self::get_requests_internal(&connection, "SELECT f.id, u.id, u.name, u.picture, u.dateOfBirth FROM friendRequests f LEFT JOIN users u ON u.id=f.userTwo WHERE userOne=? and status=?", user_id)?;
        let received = Self::get_requests_internal(&connection, "SELECT f.id, u.id, u.name, u.picture, u.dateOfBirth FROM friendRequests f LEFT JOIN users u ON u.id=f.userOne WHERE userTwo=? and status=?", user_id)?;

        Ok(Requests { sent, received })
    }

    fn get_requests_internal(
        connection: &Connection,
        query: &str,
        user_id: i64,
    ) -> Result<Vec<FriendRequest>, FriendsManagerError> {
        let mut statement = connection.prepare(query)?;
        let rows = statement.query_map(params![user_id, RequestStatus::Pending], |row| {
            <(i64, i64, String, Option<String>, Option<i64>)>::try_from(row)
        })?;
        let mut requests = Vec::new();
        for row in rows {
            let row = row?;
            requests.push(FriendRequest {
                id: row.0,
                other_user: CleanUser {
                    id: row.1,
                    name: row.2,
                    picture: row.3,
                    date_of_birth: row.4,
                },
            });
        }
        Ok(requests)
    }

    pub fn update_received_request(
        &self,
        request_id: i64,
        user_id: i64,
        status: RequestStatus,
    ) -> Result<(), FriendsManagerError> {
        let connection = self.connection.lock().unwrap();

        let mut statement = connection
            .prepare("SELECT 1 FROM friendRequests where id=? AND userTwo=? AND status=?")?;
        if !statement.exists(params![request_id, user_id, RequestStatus::Pending])? {
            return Err(FriendsManagerError::FriendRequestDoesNotExists(
                request_id, user_id,
            ));
        }

        connection.execute(
            "UPDATE friendRequests SET status=? WHERE id=?",
            params![status, request_id],
        )?;

        Ok(())
    }

    pub fn cancel_sent_request(
        &self,
        request_id: i64,
        user_id: i64,
    ) -> Result<(), FriendsManagerError> {
        let connection = self.connection.lock().unwrap();

        let mut statement = connection
            .prepare("SELECT 1 FROM friendRequests where id=? AND userOne=? AND status=?")?;
        if !statement.exists(params![request_id, user_id, RequestStatus::Pending])? {
            return Err(FriendsManagerError::FriendRequestDoesNotExists(
                request_id, user_id,
            ));
        }

        connection.execute("DELETE FROM friendRequests WHERE id=?", params![request_id])?;

        Ok(())
    }

    pub fn is_my_friend(&self, user_id: i64, friend_id: i64) -> Result<bool, FriendsManagerError> {
        let connection = self.connection.lock().unwrap();
        let mut statement = connection.prepare(
            "SELECT id FROM friendRequests WHERE userOne=? AND userTwo =? and status=? \
        UNION ALL \
        SELECT id FROM friendRequests WHERE userOne=? AND userTwo =? and status=?",
        )?;
        Ok(statement.exists(params![
            user_id,
            friend_id,
            RequestStatus::Accepted,
            friend_id,
            user_id,
            RequestStatus::Accepted
        ])?)
    }
}

#[derive(Serialize)]
#[cfg_attr(test, derive(PartialEq, Eq, Debug))]
pub struct FriendRequest {
    pub id: i64,
    pub other_user: CleanUser,
}

#[derive(Serialize)]
#[cfg_attr(test, derive(PartialEq, Eq, Debug))]
pub struct Requests {
    pub sent: Vec<FriendRequest>,
    pub received: Vec<FriendRequest>,
}

pub enum RequestStatus {
    Pending,
    Accepted,
    Declined,
}

impl ToSql for RequestStatus {
    fn to_sql(&self) -> rusqlite::Result<ToSqlOutput<'_>> {
        match self {
            RequestStatus::Pending => Ok(ToSqlOutput::from("PENDING")),
            RequestStatus::Accepted => Ok(ToSqlOutput::from("ACCEPTED")),
            RequestStatus::Declined => Ok(ToSqlOutput::from("DECLINED")),
        }
    }
}

#[cfg(test)]
mod test {
    use crate::managers::friends_manager::{
        FriendRequest, FriendsManager, FriendsManagerError, RequestStatus, Requests,
    };
    use crate::managers::test_helper::create_test_database;
    use crate::managers::users_manager::{CleanUser, UsersManager};
    use std::sync::{Arc, Mutex};

    #[test]
    fn test_create_friend_requests() {
        let connection = Arc::new(Mutex::new(create_test_database(
            "test_create_friend_requests",
        )));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();
        let two = users_manager.add_user("two", "pwd").unwrap();
        let three = users_manager.add_user("three", "pwd").unwrap();

        let friend_manager = FriendsManager::new(connection.clone()).unwrap();
        friend_manager.create_friend_request(one, "two").unwrap();
        friend_manager.create_friend_request(three, "two").unwrap();

        //Cannot re-ask
        let error = friend_manager
            .create_friend_request(one, "two")
            .unwrap_err();
        assert!(matches!(
            error,
            FriendsManagerError::FriendRequestAlreadyExists(_, _)
        ));
        //Cannot ask the other way around
        let error = friend_manager
            .create_friend_request(two, "three")
            .unwrap_err();
        assert!(matches!(
            error,
            FriendsManagerError::FriendRequestAlreadyExists(_, _)
        ));
        //Cannot ask yourself
        let error = friend_manager
            .create_friend_request(two, "two")
            .unwrap_err();
        assert!(matches!(error, FriendsManagerError::CannotAskYourself(_)));
        //Cannot ask unknown
        let error = friend_manager
            .create_friend_request(one, "unknown")
            .unwrap_err();
        assert!(matches!(error, FriendsManagerError::UnknownUser(_)));
    }

    #[test]
    fn test_get_friend_requests() {
        let connection = Arc::new(Mutex::new(create_test_database("test_get_friend_requests")));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();
        let two = users_manager.add_user("two", "pwd").unwrap();
        let three = users_manager.add_user("three", "pwd").unwrap();

        let friend_manager = FriendsManager::new(connection.clone()).unwrap();
        friend_manager.create_friend_request(one, "two").unwrap();
        friend_manager.create_friend_request(two, "three").unwrap();

        let requests = friend_manager.get_requests(one).unwrap();
        assert_eq!(
            requests,
            Requests {
                sent: vec![FriendRequest {
                    id: 1,
                    other_user: CleanUser {
                        id: 2,
                        name: "two".to_string(),
                        picture: None,
                        date_of_birth: None
                    }
                }],
                received: vec![]
            }
        );

        let requests = friend_manager.get_requests(two).unwrap();
        assert_eq!(
            requests,
            Requests {
                sent: vec![FriendRequest {
                    id: 2,
                    other_user: CleanUser {
                        id: 3,
                        name: "three".to_string(),
                        picture: None,
                        date_of_birth: None
                    }
                }],
                received: vec![FriendRequest {
                    id: 1,
                    other_user: CleanUser {
                        id: 1,
                        name: "one".to_string(),
                        picture: None,
                        date_of_birth: None
                    }
                }]
            }
        );

        let requests = friend_manager.get_requests(three).unwrap();
        assert_eq!(
            requests,
            Requests {
                sent: vec![],
                received: vec![FriendRequest {
                    id: 2,
                    other_user: CleanUser {
                        id: 2,
                        name: "two".to_string(),
                        picture: None,
                        date_of_birth: None
                    }
                }]
            }
        );
    }

    #[test]
    fn test_update_friend_request() {
        let connection = Arc::new(Mutex::new(create_test_database(
            "test_update_friend_request",
        )));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();
        let two = users_manager.add_user("two", "pwd").unwrap();

        let friend_manager = FriendsManager::new(connection.clone()).unwrap();
        friend_manager.create_friend_request(one, "two").unwrap();

        let error = friend_manager
            .update_received_request(1, one, RequestStatus::Accepted)
            .unwrap_err();
        assert!(matches!(
            error,
            FriendsManagerError::FriendRequestDoesNotExists(_, _)
        ));

        friend_manager
            .update_received_request(1, two, RequestStatus::Accepted)
            .unwrap();

        let requests = friend_manager.get_requests(one).unwrap();
        assert_eq!(
            requests,
            Requests {
                sent: vec![],
                received: vec![]
            }
        );
        let requests = friend_manager.get_requests(two).unwrap();
        assert_eq!(
            requests,
            Requests {
                sent: vec![],
                received: vec![]
            }
        );

        let friends = friend_manager.get_friends(one).unwrap();
        assert_eq!(
            friends,
            vec![CleanUser {
                id: 2,
                name: "two".to_string(),
                picture: None,
                date_of_birth: None
            }]
        );
        let friends = friend_manager.get_friends(two).unwrap();
        assert_eq!(
            friends,
            vec![CleanUser {
                id: 1,
                name: "one".to_string(),
                picture: None,
                date_of_birth: None
            }]
        );

        assert!(friend_manager.is_my_friend(one, two).unwrap());
        assert!(friend_manager.is_my_friend(two, one).unwrap());

        //Check cannot update once accepted/declined
        let error = friend_manager
            .update_received_request(1, two, RequestStatus::Declined)
            .unwrap_err();
        assert!(matches!(
            error,
            FriendsManagerError::FriendRequestDoesNotExists(_, _)
        ));
    }

    #[test]
    fn test_cancel_friend_request() {
        let connection = Arc::new(Mutex::new(create_test_database(
            "test_cancel_friend_request",
        )));
        let users_manager = UsersManager::new(connection.clone()).unwrap();
        let one = users_manager.add_user("one", "pwd").unwrap();
        let two = users_manager.add_user("two", "pwd").unwrap();

        let friend_manager = FriendsManager::new(connection.clone()).unwrap();
        friend_manager.create_friend_request(one, "two").unwrap();

        let error = friend_manager.cancel_sent_request(1, two).unwrap_err();
        assert!(matches!(
            error,
            FriendsManagerError::FriendRequestDoesNotExists(_, _)
        ));

        friend_manager.cancel_sent_request(1, one).unwrap();
        let requests = friend_manager.get_requests(one).unwrap();
        assert_eq!(
            requests,
            Requests {
                sent: vec![],
                received: vec![]
            }
        );
        let requests = friend_manager.get_requests(two).unwrap();
        assert_eq!(
            requests,
            Requests {
                sent: vec![],
                received: vec![]
            }
        );
        let friends = friend_manager.get_friends(one).unwrap();
        assert_eq!(friends, vec![]);
        let friends = friend_manager.get_friends(two).unwrap();
        assert_eq!(friends, vec![]);

        //Check cannot cancel once accepted/declined
        friend_manager.create_friend_request(one, "two").unwrap();
        friend_manager
            .update_received_request(2, two, RequestStatus::Accepted)
            .unwrap();
        let error = friend_manager.cancel_sent_request(1, two).unwrap_err();
        assert!(matches!(
            error,
            FriendsManagerError::FriendRequestDoesNotExists(_, _)
        ));
    }
}
