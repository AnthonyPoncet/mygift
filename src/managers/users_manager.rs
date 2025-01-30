use pbkdf2::pbkdf2_hmac;
use rand::RngCore;
use rusqlite::{params, Connection, OptionalExtension, Row};
use serde::Serialize;
use sha1::Sha1;
use std::sync::{Arc, Mutex};
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use tracing::log::error;
use uuid::Uuid;

#[derive(Clone)]
pub struct UsersManager {
    connection: Arc<Mutex<Connection>>,
}

#[derive(thiserror::Error, Debug)]
#[error(transparent)]
pub(crate) enum UsersManagerError {
    Sqlite(#[from] rusqlite::Error),
    #[error("User {0} already exist")]
    UserAlreadyExist(String),
    #[error("Wrong password")]
    PasswordMismatch,
    #[error("Unknown user {0}")]
    UnknownUser(String),
}

impl UsersManager {
    pub fn new(connection: Arc<Mutex<Connection>>) -> Result<Self, UsersManagerError> {
        Self::init_database(&connection)?;
        Ok(Self { connection })
    }

    fn init_database(connection: &Arc<Mutex<Connection>>) -> Result<(), UsersManagerError> {
        let connection = connection.lock().unwrap();
        connection.execute_batch("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE NOT NULL, password BLOB NOT NULL, \
        salt BLOB NOT NULL, picture TEXT, dateOfBirth LONG)")?;
        connection.execute_batch("CREATE TABLE IF NOT EXISTS reset_password (userId INTEGER NOT NULL, uuid TEXT NOT NULL, expiry INTEGER NOT NULL, FOREIGN KEY(userId) REFERENCES users(id))")?;
        Ok(())
    }

    pub fn add_user(&self, name: &str, password: &str) -> Result<i64, UsersManagerError> {
        let connection = self.connection.lock().unwrap();
        let (encoded_password, salt) = Self::generate_salt_and_encoded_password(password);
        let inserted = connection.execute("INSERT INTO users (name,password,salt,picture,dateOfBirth) VALUES (?, ?, ?, null, null) ON CONFLICT DO NOTHING", params![name, encoded_password, salt])?;
        if inserted != 1 {
            return Err(UsersManagerError::UserAlreadyExist(name.to_string()));
        }
        drop(connection);
        Ok(self.get_user(name)?.id)
    }

    pub fn edit_user(
        &self,
        user_id: i64,
        name: &str,
        picture: &Option<String>,
        date_of_birth: &Option<i64>,
    ) -> Result<(), UsersManagerError> {
        let connection = self.connection.lock().unwrap();
        let updated = connection.execute(
            "UPDATE users SET name=?, picture=?, dateOfBirth=? WHERE id=?",
            params![name, picture, date_of_birth, user_id],
        )?;
        if updated != 1 {
            return Err(UsersManagerError::UserAlreadyExist(name.to_string()));
        }
        Ok(())
    }

    pub fn get_user(&self, name: &str) -> Result<DbUser, UsersManagerError> {
        let connection = self.connection.lock().unwrap();
        Self::static_get_user(&connection, name)
    }

    pub fn static_get_user(
        connection: &Connection,
        name: &str,
    ) -> Result<DbUser, UsersManagerError> {
        let mut stmt = connection.prepare(
            "SELECT id, name, password, salt, picture, dateOfBirth FROM users WHERE name=?",
        )?;
        let user = stmt
            .query_row([name], |row| <_>::try_from(row))
            .optional()?
            .ok_or(UsersManagerError::UnknownUser(name.to_string()))?;
        Ok(user)
    }

    pub fn check_password(
        &self,
        name: &str,
        password: &str,
    ) -> Result<CleanUser, UsersManagerError> {
        let user = self.get_user(name)?;

        if Self::encode_password(password, &user.salt) == user.encoded_password {
            Ok(user.into())
        } else {
            Err(UsersManagerError::PasswordMismatch)
        }
    }

    fn generate_salt_and_encoded_password(password: &str) -> ([u8; 32], [u8; 16]) {
        let mut rand = rand::thread_rng();
        let mut salt = [0u8; 16];
        rand.fill_bytes(&mut salt);

        (Self::encode_password(password, &salt), salt)
    }

    pub fn create_password_reset_request(&self, user_id: i64) -> Result<String, UsersManagerError> {
        let connection = self.connection.lock().unwrap();
        connection.execute("DELETE FROM reset_password WHERE userId=?", [user_id])?;

        let uuid = Uuid::new_v4().to_string();
        let expiry = SystemTime::now()
            .checked_add(Duration::from_secs(60 * 60))
            .unwrap()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs();
        connection.execute(
            "INSERT INTO reset_password(userId, uuid, expiry) VALUES (?,?,?)",
            params![user_id, &uuid, expiry],
        )?;
        Ok(uuid)
    }

    pub fn change_user_password(
        &self,
        user_id: i64,
        uuid: &str,
        password: &str,
    ) -> Result<(), UsersManagerError> {
        let connection = self.connection.lock().unwrap();
        let mut stmt =
            connection.prepare("SELECT expiry FROM reset_password WHERE userId=? and uuid=?")?;
        let Some(expiry) = stmt
            .query_row(params![user_id, uuid], |row| row.get::<_, i64>(0))
            .optional()?
        else {
            error!("UUID {uuid} does not exist or do not belong to the user {user_id}");
            connection.execute("DELETE FROM reset_password WHERE uuid=?", [uuid])?;
            connection.execute("DELETE FROM reset_password WHERE userId=?", [user_id])?;
            return Ok(());
        };

        if SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs()
            > expiry as u64
        {
            error!("Took too long to reset the password for {uuid} and user {user_id}");
            connection.execute("DELETE FROM reset_password WHERE uuid=?", [uuid])?;
            connection.execute("DELETE FROM reset_password WHERE userId=?", [user_id])?;
            return Ok(());
        }

        connection.execute("DELETE FROM reset_password WHERE uuid=?", [uuid])?;
        connection.execute("DELETE FROM reset_password WHERE userId=?", [user_id])?;

        let (encoded_password, salt) = Self::generate_salt_and_encoded_password(password);
        connection.execute(
            "UPDATE users SET password=?, salt=? WHERE id=?",
            params![encoded_password, salt, user_id],
        )?;

        Ok(())
    }

    fn encode_password(password: &str, salt: &[u8; 16]) -> [u8; 32] {
        let mut to_check_encoded = [0u8; 32];
        pbkdf2_hmac::<Sha1>(password.as_bytes(), salt, 10000, &mut to_check_encoded);
        to_check_encoded
    }
}

#[cfg_attr(test, derive(Eq, PartialEq, Debug))]
pub(crate) struct DbUser {
    pub(crate) id: i64,
    pub(crate) name: String,
    pub(crate) encoded_password: [u8; 32],
    pub(crate) salt: [u8; 16],
    pub(crate) picture: Option<String>,
    pub(crate) date_of_birth: Option<i64>,
}

#[cfg(test)]
impl DbUser {
    fn test_eq(
        &self,
        id: i64,
        name: &str,
        picture: Option<String>,
        date_of_birth: Option<i64>,
    ) -> bool {
        self.id == id
            && self.name == name
            && self.picture == picture
            && self.date_of_birth == date_of_birth
    }
}

impl<'a> TryFrom<&Row<'a>> for DbUser {
    type Error = rusqlite::Error;

    fn try_from(row: &Row<'a>) -> Result<Self, Self::Error> {
        Ok(Self {
            id: row.get(0)?,
            name: row.get(1)?,
            encoded_password: row.get(2)?,
            salt: row.get(3)?,
            picture: row.get(4)?,
            date_of_birth: row.get(5)?,
        })
    }
}

#[derive(Serialize)]
#[cfg_attr(test, derive(Eq, PartialEq, Debug))]
pub(crate) struct CleanUser {
    pub(crate) id: i64,
    pub(crate) name: String,
    pub(crate) picture: Option<String>,
    pub(crate) date_of_birth: Option<i64>,
}

impl From<DbUser> for CleanUser {
    fn from(value: DbUser) -> Self {
        CleanUser {
            id: value.id,
            name: value.name,
            picture: value.picture,
            date_of_birth: value.date_of_birth,
        }
    }
}

impl<'a> TryFrom<&Row<'a>> for CleanUser {
    type Error = rusqlite::Error;

    fn try_from(row: &Row<'a>) -> Result<Self, Self::Error> {
        Ok(Self {
            id: row.get(0)?,
            name: row.get(1)?,
            picture: row.get(2)?,
            date_of_birth: row.get(3)?,
        })
    }
}

#[cfg(test)]
mod test {
    use crate::managers::test_helper::create_test_database;
    use crate::managers::users_manager::{UsersManager, UsersManagerError};
    use std::sync::{Arc, Mutex};

    #[test]
    fn test_add_user() {
        let connection = Arc::new(Mutex::new(create_test_database("test_add_user")));
        let users_manager = UsersManager::new(connection).unwrap();

        let id = users_manager.add_user("test", "strong_pwd").unwrap();
        assert_eq!(id, 1);
        let user = users_manager.get_user("test").unwrap();
        assert!(user.test_eq(1, "test", None, None), "Got: {user:?}");
        assert!(users_manager.check_password("test", "strong_pwd").is_ok());

        let id = users_manager.add_user("test2", "weak_pwd").unwrap();
        assert_eq!(id, 2);
        let user = users_manager.get_user("test2").unwrap();
        assert!(user.test_eq(2, "test2", None, None), "Got: {user:?}");
        assert!(users_manager.check_password("test2", "weak_pwd").is_ok());

        let error = users_manager
            .add_user("test2", "already_there")
            .unwrap_err();
        assert!(matches!(error, UsersManagerError::UserAlreadyExist(_)));
    }

    #[test]
    fn test_get_unknown_user() {
        let connection = Arc::new(Mutex::new(create_test_database("test_get_unknown_user")));
        let users_manager = UsersManager::new(connection).unwrap();

        users_manager.add_user("test", "strong_pwd").unwrap();
        let user = users_manager.get_user("test").unwrap();
        assert!(user.test_eq(1, "test", None, None), "Got: {user:?}");
        assert!(users_manager.check_password("test", "strong_pwd").is_ok());

        let error = users_manager.get_user("unknown").unwrap_err();
        assert!(matches!(error, UsersManagerError::UnknownUser(_)));
    }

    #[test]
    fn test_change_password() {
        let connection = Arc::new(Mutex::new(create_test_database("test_change_password")));
        let users_manager = UsersManager::new(connection).unwrap();

        let id = users_manager.add_user("test", "strong_pwd").unwrap();
        assert_eq!(id, 1);
        let user = users_manager.get_user("test").unwrap();
        assert!(user.test_eq(1, "test", None, None), "Got: {user:?}");
        assert!(users_manager.check_password("test", "strong_pwd").is_ok());

        let uuid = users_manager.create_password_reset_request(1).unwrap();

        users_manager
            .change_user_password(id, &uuid, "new_strong")
            .unwrap();
        assert!(users_manager.check_password("test", "new_strong").is_ok());
        assert!(users_manager.check_password("test", "strong_pwd").is_err());
    }

    #[test]
    fn test_change_password_unknown_uuid() {
        let connection = Arc::new(Mutex::new(create_test_database(
            "test_change_password_unknown_uuid",
        )));
        let users_manager = UsersManager::new(connection).unwrap();

        let id = users_manager.add_user("test", "strong_pwd").unwrap();
        assert_eq!(id, 1);
        let user = users_manager.get_user("test").unwrap();
        assert!(user.test_eq(1, "test", None, None), "Got: {user:?}");
        assert!(users_manager.check_password("test", "strong_pwd").is_ok());

        users_manager
            .change_user_password(id, "random_uuid", "new_strong")
            .unwrap();
        assert!(users_manager.check_password("test", "new_strong").is_err());
        assert!(users_manager.check_password("test", "strong_pwd").is_ok());
    }
}
