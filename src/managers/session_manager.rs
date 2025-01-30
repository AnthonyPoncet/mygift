use rand::distributions::Alphanumeric;
use rand::Rng;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};

#[derive(Clone, Default)]
pub struct SessionManager {
    pub(crate) sessions: Arc<Mutex<HashMap<i64, String>>>,
}

impl SessionManager {
    pub fn generate_session(&mut self, user_id: i64) -> String {
        let mut sessions = self.sessions.lock().unwrap();
        {
            if let Some(current_session) = sessions.get(&user_id) {
                return current_session.to_string();
            }
        }

        let session = rand::thread_rng()
            .sample_iter(Alphanumeric)
            .take(128)
            .map(char::from)
            .collect::<String>();

        sessions.insert(user_id, session.clone());
        session
    }

    pub fn set_common_session(&mut self, user_id: i64, other_user_id: i64) -> Option<()> {
        let mut sessions = self.sessions.lock().unwrap();
        let other_session = {
            let other_session = sessions.get(&other_user_id)?;
            other_session.to_string()
        };
        sessions.insert(user_id, other_session);
        Some(())
    }

    pub fn get_session(&self, user_id: i64) -> Option<String> {
        let sessions = self.sessions.lock().unwrap();
        sessions.get(&user_id).map(|s| s.to_string())
    }

    pub fn delete_session(&self, user_id: i64) {
        let mut sessions = self.sessions.lock().unwrap();
        sessions.remove(&user_id).map(|s| s.to_string());
    }
}
