//! Blog post upload, edit, and deletion workflows.
//!
//! Disclaimer: Written by an LLM. This file prompts for post metadata,
//! uploads and rewrites local JPEG references, and performs post CRUD calls.

use crate::{
    client::{ApiClient, PostPayload},
    file_io::collect_package,
};
use dialoguer::{Confirm, Input};
use std::fs;

pub fn upload(
    client: &ApiClient,
    dir: &str,
    id: Option<i64>,
) -> Result<(), Box<dyn std::error::Error>> {
    let package = collect_package(dir)?;
    let mut content = fs::read_to_string(&package.markdown_file)?;
    for image in &package.image_files {
        let upload = client.upload_image(image)?;
        println!(
            "Uploaded {} as {} ({}, {})",
            upload.original_filename,
            upload.uuid,
            upload.content_type.as_deref().unwrap_or("unknown type"),
            upload.time_uploaded
        );
        let name = image
            .file_name()
            .ok_or("Image has no filename")?
            .to_string_lossy();
        content = content.replace(&name.to_string(), &upload.url);
    }
    fs::write(&package.markdown_file, &content)?;

    let title: String = Input::new().with_prompt("Title").interact_text()?;
    let summary: String = Input::new()
        .with_prompt("Summary")
        .allow_empty(true)
        .interact_text()?;
    let published = Confirm::new()
        .with_prompt("Published?")
        .default(true)
        .interact()?;
    let payload = PostPayload {
        title,
        summary,
        content,
        is_published: published,
    };
    match id {
        Some(existing) => client.update_post(existing, &payload)?,
        None => {
            let created = client.create_post(&payload)?;
            println!("Created post with ID {} and slug '{}'.", created.id, created.slug);
        }
    }
    println!("Blog post saved.");
    Ok(())
}

pub fn delete(client: &ApiClient, id: i64) -> Result<(), Box<dyn std::error::Error>> {
    if Confirm::new()
        .with_prompt(format!("Delete post with ID {id}?"))
        .default(false)
        .interact()?
    {
        client.delete_post(id)?;
        println!("Blog post deleted.");
    }
    Ok(())
}
