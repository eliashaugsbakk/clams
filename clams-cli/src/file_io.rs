//! Blog package discovery and local image validation.
//!
//! Disclaimer: Written by an LLM. This file discovers one Markdown file,
//! collects JPEG assets, and validates their dimensions before upload.

use image::ImageReader;
use std::fs;
use std::path::PathBuf;

pub struct BlogPackage {
    pub markdown_file: PathBuf,
    pub image_files: Vec<PathBuf>,
}

pub fn validate_image(path: &std::path::Path) -> Result<(), Box<dyn std::error::Error>> {
    let is_jpeg = path
        .extension()
        .and_then(|extension| extension.to_str())
        .is_some_and(|extension| extension.eq_ignore_ascii_case("jpeg"));
    if !path.is_file() || !is_jpeg {
        return Err(format!("Image must be a .jpeg file: {}", path.display()).into());
    }

    let image = ImageReader::open(path)?.with_guessed_format()?.decode()?;
    if image.width() > 2000 || image.height() > 2000 {
        return Err(format!(
            "Image {} exceeds the server limit of 2000x2000 pixels",
            path.display()
        )
        .into());
    }
    Ok(())
}

pub fn collect_images(input_path: &str) -> Result<Vec<PathBuf>, Box<dyn std::error::Error>> {
    let path = std::path::Path::new(input_path);
    if !path.is_dir() {
        return Err(format!("Image directory does not exist: {}", path.display()).into());
    }

    let mut images = Vec::new();
    for entry in fs::read_dir(path)? {
        let entry_path = entry?.path();
        let is_jpeg = entry_path
            .extension()
            .and_then(|extension| extension.to_str())
            .is_some_and(|extension| extension.eq_ignore_ascii_case("jpeg"));
        if is_jpeg {
            images.push(entry_path);
        }
    }
    images.sort();

    if images.is_empty() {
        return Err(format!("No .jpeg images found in {}", path.display()).into());
    }

    for image_path in &images {
        validate_image(image_path)?;
    }
    Ok(images)
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
        validate_image(image_path)?;
    }

    Ok(BlogPackage {
        markdown_file: md_files.remove(0),
        image_files: jpeg_files,
    })
}
