//! Blog package discovery and local image validation.
//!
//! Disclaimer: This file was created and edited by an AI model.

use image::ImageReader;
use std::fs;
use std::path::PathBuf;

pub struct BlogPackage {
    pub markdown_file: PathBuf,
    pub image_files: Vec<PathBuf>,
}

pub fn collect_package(input_path: &str) -> Result<BlogPackage, Box<dyn std::error::Error>> {
    let mut md_files = Vec::new();
    let mut jpeg_files = Vec::new();

    let path = std::path::Path::new(input_path);
    if path.is_dir() {
        for entry in fs::read_dir(path)? {
            let entry_path = entry?.path();
            if let Some(ext) = entry_path.extension().and_then(|e| e.to_str()) {
                if ext.eq_ignore_ascii_case("md") {
                    md_files.push(entry_path);
                } else if ext.eq_ignore_ascii_case("jpeg") {
                    jpeg_files.push(entry_path);
                }
            }
        }
    }

    if md_files.len() != 1 {
        return Err(format!(
            "Error: Expected exactly 1 .md file, found {}",
            md_files.len()
        )
        .into());
    }

    for image_path in &jpeg_files {
        let image = ImageReader::open(image_path)?
            .with_guessed_format()?
            .decode()?;
        if image.width() > 2000 || image.height() > 2000 {
            return Err(format!(
                "Image {} exceeds the server limit of 2000x2000 pixels",
                image_path.display()
            )
            .into());
        }
    }

    Ok(BlogPackage {
        markdown_file: md_files.remove(0),
        image_files: jpeg_files,
    })
}
