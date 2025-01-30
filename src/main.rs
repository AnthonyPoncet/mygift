use crate::configuration::Configuration;
use crate::managers::events_manager::EventsManager;
use crate::managers::friends_manager::FriendsManager;
use crate::managers::jwt_manager::JwtManager;
use crate::managers::session_manager::SessionManager;
use crate::managers::users_manager::UsersManager;
use crate::managers::wishlist_manager::WishlistManager;
use crate::routes::files::resize_file;
use crate::routes::{create_api_routes, AppState};
use axum::Router;
use axum_server::tls_rustls::RustlsConfig;
use std::fs;
use std::net::SocketAddr;
use std::path::PathBuf;
use std::sync::{Arc, Mutex};
use std::time::SystemTime;
use axum::http::header::{ACCESS_CONTROL_ALLOW_ORIGIN, AUTHORIZATION, CONTENT_TYPE};
use axum::http::Method;
use tower_http::cors::CorsLayer;
use tower_http::services::ServeDir;
use tower_http::trace::TraceLayer;
use tracing::debug;
use tracing_subscriber::layer::SubscriberExt;
use tracing_subscriber::util::SubscriberInitExt;

mod auth_middleware;
mod configuration;
mod error_catcher;
mod managers;
mod routes;

#[tokio::main()]
async fn main() {
    let configuration_file = fs::read_to_string("configuration.json").unwrap();
    let configuration: Configuration = serde_json::from_str(&configuration_file).unwrap();

    let filter = tracing_subscriber::EnvFilter::builder()
        .parse(&configuration.log_level)
        .unwrap();
    tracing_subscriber::registry()
        .with(filter)
        .with(tracing_subscriber::fmt::layer())
        .init();

    //Migration from java
    /*{
        let connection = rusqlite::Connection::open(DATABASE_PATH).unwrap();
        //connection.execute_batch("ALTER TABLE friendRequest RENAME TO friendRequests").unwrap();
        //connection.execute_batch("UPDATE users SET picture=NULL WHERE picture=''").unwrap();
        //connection.execute_batch("UPDATE users SET dateOfBirth=NULL WHERE dateOfBirth=0").unwrap();
        //connection.execute_batch("ALTER TABLE gifts ADD COLUMN reservedBy INTEGER").unwrap();
        let mut statement = connection.prepare("SELECT giftId, userId FROM friendActionOnGift").unwrap();
        let mut rows = statement.query(params![]).unwrap();
        while let Some(row) = rows.next().unwrap() {
            let gift_id = row.get::<_, i64>(0).unwrap();
            let user_id = row.get::<_, i64>(0).unwrap();
            connection.execute("UPDATE gifts SET reservedBy=? WHERE id=?", params![user_id, gift_id]).unwrap();
        }
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
            .allow_methods([Method::GET, Method::POST, Method::DELETE, Method::PATCH, Method::PUT])
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
