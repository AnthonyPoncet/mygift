use crate::configuration::Configuration;
use crate::error_catcher::AppError;
use axum::body::Body;
use axum::extract::{Multipart, Path, State};
use axum::http::{header, StatusCode};
use axum::response::IntoResponse;
use axum::Json;
use image::imageops::FilterType;
use image::ImageReader;
use serde::Serialize;
use std::fs;
use std::io::Cursor;
use std::path::PathBuf;
use std::sync::Arc;
use std::time::SystemTime;
use tracing::log::debug;
use tracing::warn;

pub(crate) async fn get_file(
    State(configuration): State<Arc<Configuration>>,
    Path(file_name): Path<String>,
) -> impl IntoResponse {
    //Could be async
    let root_path = PathBuf::from(&configuration.upload_file_storage);
    let root_output_path = root_path.parent().unwrap().to_path_buf().join("tmp_rs");
    let original_file = root_path.join(file_name);
    let Some(output_file) = resize_file(&original_file, &root_output_path) else {
        return Err(StatusCode::NOT_FOUND);
    };
    let Ok(content) = fs::read(output_file) else {
        return Err(StatusCode::NOT_FOUND);
    };
    let body = Body::from(content);
    let headers = [(header::CONTENT_TYPE, "image")];
    Ok((headers, body))
}

#[derive(Serialize)]
pub(crate) struct FileName {
    name: String,
}

pub(crate) async fn upload_file(
    State(configuration): State<Arc<Configuration>>,
    mut multipart: Multipart,
) -> Result<(StatusCode, Json<FileName>), AppError> {
    //Could be async
    while let Some(field) = multipart.next_field().await.unwrap() {
        let name = field.name().unwrap().to_string();
        let data = field.bytes().await.unwrap();

        if name == "file" {
            let Ok(data) = ImageReader::new(Cursor::new(data)).with_guessed_format() else {
                return Err(AppError::Conflict);
            };
            let Ok(image) = data.decode() else {
                return Err(AppError::Conflict);
            };
            let image = image.resize(300, 300, FilterType::Triangle);
            let file_name = format!(
                "upload-{}.png",
                SystemTime::now()
                    .duration_since(SystemTime::UNIX_EPOCH)
                    .unwrap()
                    .as_millis()
            );
            let output_file = PathBuf::from(&configuration.upload_file_storage).join(&file_name);
            if let Err(error) = image.save(&output_file) {
                debug!("Could not save file upload to {output_file:?}: {error}");
                return Err(AppError::Conflict);
            }

            return Ok((StatusCode::OK, Json(FileName { name: file_name })));
        }
    }

    Err(AppError::Conflict)
}

pub fn resize_file(original_file: &PathBuf, tmp_folder: &PathBuf) -> Option<PathBuf> {
    let Some(file_stem) = original_file.file_stem() else {
        debug!("Could not read stem of {original_file:?}");
        return None;
    };

    let file_name_oss = file_stem.to_ascii_lowercase();
    let file_name = file_name_oss.to_string_lossy();
    let Some(extension) = original_file.extension() else {
        debug!("Could not read extension of {original_file:?}");
        return None;
    };
    let extension_oss = extension.to_ascii_lowercase();
    let extension = extension_oss.to_string_lossy();
    if extension != "jpeg" && extension != "jpg" && extension != "png" && extension != "webp" {
        let output_file = tmp_folder.join(format!("{file_name}.{extension}"));
        if output_file.exists() {
            return Some(output_file);
        }
        warn!("Format not supported {file_name} - copy the file over as is");
        if fs::copy(original_file, &output_file).is_err() {
            return None;
        }
        return Some(output_file);
    }
    let mut output_file = tmp_folder.clone();
    if extension == "jpeg" || extension == "jpg" {
        output_file.push(format!("{file_name}.jpg"));
    } else {
        output_file.push(format!("{file_name}.webp"));
    }
    if output_file.exists() {
        return Some(output_file);
    }

    let image_reader = match ImageReader::open(original_file) {
        Ok(ir) => ir,
        Err(error) => {
            debug!("Could not read {original_file:?}: {error}");
            return None;
        }
    };
    let img = match image_reader.decode() {
        Ok(img) => img,
        Err(error) => {
            debug!("Could not decode {original_file:?}: {error}");
            return None;
        }
    };
    let img = img.resize(300, 300, FilterType::Triangle);
    if let Err(error) = img.save(&output_file) {
        debug!("Could not save file {original_file:?} to {output_file:?}: {error}");
        return None;
    }

    Some(output_file)
}
