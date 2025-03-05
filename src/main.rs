use crate::commands::{Cli, Commands};
use crate::configuration::Configuration;
use crate::managers::events_manager::EventsManager;
use crate::managers::friends_manager::FriendsManager;
use crate::managers::jwt_manager::JwtManager;
use crate::managers::session_manager::SessionManager;
use crate::managers::users_manager::UsersManager;
use crate::managers::wishlist_manager::WishlistManager;
use crate::routes::files::resize_file;
use crate::routes::{create_api_routes, AppState};
use axum::extract::DefaultBodyLimit;
use axum::http::header::{ACCESS_CONTROL_ALLOW_ORIGIN, AUTHORIZATION, CONTENT_TYPE};
use axum::http::Method;
use axum::Router;
use axum_server::tls_rustls::RustlsConfig;
use clap::Parser;
use rusqlite::params;
use std::collections::HashSet;
use std::fs;
use std::net::SocketAddr;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use std::time::SystemTime;
use tower_http::cors::CorsLayer;
use tower_http::services::ServeDir;
use tower_http::trace::TraceLayer;
use tracing::{debug, info};
use tracing_subscriber::layer::SubscriberExt;
use tracing_subscriber::util::SubscriberInitExt;

mod auth_middleware;
mod commands;
mod configuration;
mod error_catcher;
mod managers;
mod routes;

#[tokio::main()]
async fn main() {
    let cli = Cli::parse();

    let configuration_file =
        fs::read_to_string(cli.config.unwrap_or("configuration.json".into())).unwrap();
    let configuration: Configuration = serde_json::from_str(&configuration_file).unwrap();

    let filter = tracing_subscriber::EnvFilter::builder()
        .parse(&configuration.log_level)
        .unwrap();
    tracing_subscriber::registry()
        .with(filter)
        .with(tracing_subscriber::fmt::layer())
        .init();

    match cli.command {
        Some(Commands::CleanPictures) => {
            debug!("Cleaning pictures");
            let connection = rusqlite::Connection::open(&configuration.database).unwrap();
            let mut pictures = HashSet::new();
            let mut statement = connection
                .prepare("SELECT picture FROM users WHERE picture IS NOT NULL")
                .unwrap();
            let mut rows = statement.query(params![]).unwrap();
            while let Some(row) = rows.next().unwrap() {
                pictures.insert(row.get::<_, String>(0).unwrap());
            }
            let mut statement = connection
                .prepare("SELECT picture FROM gifts WHERE picture IS NOT NULL")
                .unwrap();
            let mut rows = statement.query(params![]).unwrap();
            while let Some(row) = rows.next().unwrap() {
                pictures.insert(row.get::<_, String>(0).unwrap());
            }

            debug!("All used pictures:({}) {pictures:?}", pictures.len());
            let mut to_delete = Vec::new();
            for file in PathBuf::from(&configuration.upload_file_storage)
                .read_dir()
                .unwrap()
            {
                let file = file.unwrap();
                let file_path = file.path();
                let file_name = file_path.file_name().unwrap().to_str().unwrap();
                if !pictures.remove(file_name) {
                    to_delete.push(file_path);
                }
            }
            debug!("All missing pictures: ({}) {pictures:?}", pictures.len());
            debug!(
                "All to delete pictures: ({}) {to_delete:?}",
                to_delete.len()
            );
            let tmp_path = PathBuf::from(configuration.upload_file_storage)
                .parent()
                .unwrap()
                .join("tmp_rs");
            for file in to_delete {
                let stem = file.file_stem().unwrap().to_str().unwrap().to_lowercase();
                let extension = file.extension().unwrap().to_str().unwrap().to_lowercase();
                if extension == "jpeg" || extension == "jpg" {
                    fs::remove_file(tmp_path.join(format!("{stem}.jpg"))).unwrap();
                } else if extension == "png" || extension == "webp" {
                    fs::remove_file(tmp_path.join(format!("{stem}.webp"))).unwrap();
                } else {
                    fs::remove_file(tmp_path.join(format!("{stem}.{extension}"))).unwrap();
                }
                fs::remove_file(file).unwrap()
            }
            return;
        }
        Some(Commands::ResetPassword { name }) => {
            debug!("Resetting password of {name}");
            let connection = Arc::new(Mutex::new(
                rusqlite::Connection::open(&configuration.database).unwrap(),
            ));
            let users_manager = UsersManager::new(connection).unwrap();
            let uuid = users_manager.create_password_reset_request(&name).unwrap();
            info!("Request have been created: {uuid}");
            return;
        }
        Some(Commands::Server) | None => {}
    }

    //Migration from java
    /*{
        let connection = rusqlite::Connection::open(DATABASE_PATH).unwrap();
        //connection.execute_batch("ALTER TABLE friendRequest RENAME TO friendRequests").unwrap();
        //connection.execute_batch("UPDATE users SET picture=NULL WHERE picture=''").unwrap();
        //connection.execute_batch("UPDATE users SET dateOfBirth=NULL WHERE dateOfBirth=0").unwrap();
        //connection.execute_batch("UPDATE gifts SET picture=NULL WHERE picture=''").unwrap();
        //connection.execute_batch("ALTER TABLE gifts ADD COLUMN reservedBy INTEGER").unwrap();
        let mut statement = connection.prepare("SELECT giftId, userId FROM friendActionOnGift").unwrap();
        let mut rows = statement.query(params![]).unwrap();
        while let Some(row) = rows.next().unwrap() {
            let gift_id = row.get::<_, i64>(0).unwrap();
            let user_id = row.get::<_, i64>(0).unwrap();
            connection.execute("UPDATE gifts SET reservedBy=? WHERE id=?", params![user_id, gift_id]).unwrap();
        }
    }*/

    /*{
        let connection = rusqlite::Connection::open(&configuration.database).unwrap();
        let mut statement = connection.prepare("SELECT distinct(categoryId) FROM gifts order by categoryId").unwrap();
        let rows: Vec<_> = statement.query_map(params![], |r| r.get::<_, i64>(0)).unwrap().collect();
        let mut statement_not_secret = connection.prepare("SELECT id FROM gifts where categoryId=? AND secret=FALSE order by rank").unwrap();
        let mut statement_secret = connection.prepare("SELECT id FROM gifts where categoryId=? AND secret=TRUE order by rank").unwrap();
        for row in rows {
            let category = row.unwrap();
            debug!("Reorder category {category}");
            let rows = statement_not_secret.query_map(params![category], |r| r.get::<_, i64>(0)).unwrap();
            for (count, row) in rows.enumerate() {
                let gift = row.unwrap();
                debug!("Update not secret {gift} to {count}");
                connection.execute("UPDATE gifts SET rank=? WHERE id=?", params![count, gift]).unwrap();
            }
            let rows = statement_secret.query_map(params![category], |r| r.get::<_, i64>(0)).unwrap();
            for (count, row) in rows.enumerate() {
                let gift = row.unwrap();
                debug!("Update secret {gift} to {}", count+100001);
                connection.execute("UPDATE gifts SET rank=? WHERE id=?", params![count+100001, gift]).unwrap();
            }
        }
        return;
    }*/

    //Should it be done at starts ?
    let root_path = PathBuf::from(&configuration.upload_file_storage);
    let root_output_path = root_path.parent().unwrap().to_path_buf().join("tmp_rs");
    fs::create_dir_all(&root_output_path).unwrap();
    let dir = root_path.read_dir().unwrap();

    let grand_total = SystemTime::now();
    for file in dir {
        let file = file.unwrap();
        let file_path = file.path();
        resize_file(&file_path, &root_output_path);
        debug!("File resized");
    }
    debug!(
        "Process all in {} ms",
        grand_total.elapsed().unwrap().as_millis()
    );

    let connection = Arc::new(Mutex::new(
        rusqlite::Connection::open(&configuration.database).unwrap(),
    ));

    let users_manager = UsersManager::new(connection.clone()).unwrap();
    let jwt_manager = Arc::new(JwtManager::default());
    let session_manager = SessionManager::default();
    let friends_manager = FriendsManager::new(connection.clone()).unwrap();
    let events_manager = EventsManager {
        friends_manager: friends_manager.clone(),
    };
    let wishlist_manager = WishlistManager::new(connection.clone()).unwrap();
    let serve_dir = ServeDir::new(&configuration.front_dir);
    let configuration = Arc::new(configuration);

    let app_state = AppState {
        users_manager,
        jwt_manager: jwt_manager.clone(),
        session_manager: session_manager.clone(),
        events_manager,
        friends_manager,
        wishlist_manager,
        configuration: configuration.clone(),
    };

    let cors_layer = if configuration.debug {
        CorsLayer::permissive()
    } else {
        CorsLayer::new()
            .allow_methods([
                Method::GET,
                Method::POST,
                Method::DELETE,
                Method::PATCH,
                Method::PUT,
            ])
            .allow_headers([AUTHORIZATION, CONTENT_TYPE, ACCESS_CONTROL_ALLOW_ORIGIN])
            .allow_credentials(true)
            .allow_origin(["https://www.druponps.fr".parse().unwrap()])
    };

    let app = Router::new()
        .nest(
            "/api",
            create_api_routes(session_manager, jwt_manager, configuration.clone()),
        )
        .with_state(app_state)
        .layer(TraceLayer::new_for_http())
        .layer(cors_layer)
        .layer(DefaultBodyLimit::disable())
        .nest_service("/signin", serve_dir.clone())
        .nest_service("/signup", serve_dir.clone())
        .nest_service("/mywishlist", serve_dir.clone())
        .nest_service("/myfriends", serve_dir.clone())
        .nest_service("/friend/{a}", serve_dir.clone())
        .nest_service("/manageaccount", serve_dir.clone())
        .nest_service("/changeaccount", serve_dir.clone())
        .fallback_service(serve_dir.clone());

    if configuration.debug {
        let listener = tokio::net::TcpListener::bind("0.0.0.0:4242").await.unwrap();
        axum::serve(listener, app).await.unwrap();
    } else {
        let addr = SocketAddr::from(([0, 0, 0, 0], 443));
        let config = RustlsConfig::from_pem_file(
            configuration.cert_pem.as_ref().unwrap(),
            configuration.key_pem.as_ref().unwrap(),
        )
        .await
        .unwrap();
        axum_server::bind_rustls(addr, config)
            .serve(app.into_make_service())
            .await
            .unwrap()
    }
}
