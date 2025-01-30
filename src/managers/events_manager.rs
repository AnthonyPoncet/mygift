use crate::managers::friends_manager::{FriendsManager, FriendsManagerError};
use chrono::{DateTime, Datelike, Months, NaiveDate, Utc};
use serde::Serialize;
use std::time::SystemTime;

#[derive(Clone)]
pub struct EventsManager {
    pub friends_manager: FriendsManager,
}

#[derive(thiserror::Error, Debug)]
#[error(transparent)]
pub(crate) enum EventsManagerError {
    FriendsManager(#[from] FriendsManagerError),
}

impl EventsManager {
    pub fn get_events(&self, user_id: i64) -> Result<Vec<Event>, EventsManagerError> {
        let friends = self.friends_manager.get_friends(user_id)?;

        let now: DateTime<Utc> = SystemTime::now().into();
        let in_six_month = now.checked_add_months(Months::new(6)).unwrap();

        let mut events = Vec::new();
        for friend in friends {
            if let Some(date_of_birth) = friend.date_of_birth {
                let date_of_birth = DateTime::from_timestamp(date_of_birth, 0).unwrap();
                let mut birthday: DateTime<Utc> = DateTime::from_naive_utc_and_offset(
                    NaiveDate::from_ymd_opt(now.year(), date_of_birth.month(), date_of_birth.day())
                        .unwrap()
                        .into(),
                    *now.offset(),
                );
                if birthday < now {
                    birthday = birthday.checked_add_months(Months::new(12)).unwrap();
                }
                if birthday < in_six_month {
                    events.push(Event {
                        kind: EventKind::Birthday,
                        date: birthday.timestamp(),
                        name: Some(friend.name),
                        picture: friend.picture,
                        birth: friend.date_of_birth,
                    })
                }
            }
        }

        let mut christmas: DateTime<Utc> = DateTime::from_naive_utc_and_offset(
            NaiveDate::from_ymd_opt(now.year(), 12, 25).unwrap().into(),
            *now.offset(),
        );
        if christmas < now {
            christmas = christmas.checked_add_months(Months::new(12)).unwrap();
        }
        if christmas < in_six_month {
            events.push(Event {
                kind: EventKind::Christmas,
                date: christmas.timestamp(),
                name: None,
                picture: None,
                birth: None,
            })
        }

        events.sort_by_key(|e| e.date);

        Ok(events)
    }
}

#[derive(Serialize)]
pub enum EventKind {
    Birthday,
    Christmas,
}

#[derive(Serialize)]
pub struct Event {
    kind: EventKind,
    date: i64,
    name: Option<String>,
    picture: Option<String>,
    birth: Option<i64>,
}
