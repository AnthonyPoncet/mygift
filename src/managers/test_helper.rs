use rusqlite::Connection;
use std::env::current_dir;
use std::fs::{create_dir_all, remove_file};

pub(crate) fn create_test_database(test_name: &str) -> Connection {
    let mut path = current_dir().unwrap();
    path.push("test_databases");
    create_dir_all(&path).ok();

    path.push(format!("{test_name}.sqlite"));
    if path.exists() {
        remove_file(&path).unwrap();
    }

    let connection = Connection::open(path).unwrap();
    connection
        .execute_batch("PRAGMA foreign_keys = ON;")
        .unwrap();
    connection
}
