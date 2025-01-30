use serde::Deserialize;

#[derive(Clone, Deserialize)]
pub struct Configuration {
    pub debug: bool,
    pub log_level: String,

    pub database: String,
    pub upload_file_storage: String,
    pub front_dir: String,

    pub cert_pem: Option<String>,
    pub key_pem: Option<String>,
}
