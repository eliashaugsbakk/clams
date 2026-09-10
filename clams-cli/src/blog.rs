//! Blog post upload, edit, and deletion workflows.
//!
//! Disclaimer: This file was created and edited by an AI model.

use crate::{
    client::{ApiClient, PostPayload},
    file_io::collect_package,
};
use dialoguer::{Confirm, Input};
use std::fs;

pub fn upload(
    client: &ApiClient,
    dir: &str,
    slug: Option<&str>,
) -> Result<(), Box<dyn std::error::Error>> {
    let package = collect_package(dir)?;
    let mut content = fs::read_to_string(&package.markdown_file)?;
    for image in &package.image_files {
        let upload = client.upload_image(image)?;
        let name = image
            .file_name()
            .ok_or("Image has no filename")?
            .to_string_lossy();
        content = content.replace(&name.to_string(), &format!("/media/{}", upload.uuid));
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
    match slug {
        Some(existing) => client.update_post(existing, &payload)?,
        None => client.create_post(&payload)?,
    }
    println!("Blog post saved.");
    Ok(())
}

pub fn delete(client: &ApiClient, slug: &str) -> Result<(), Box<dyn std::error::Error>> {
    if Confirm::new()
        .with_prompt(format!("Delete post '{slug}'?"))
        .default(false)
        .interact()?
    {
        client.delete_post(slug)?;
        println!("Blog post deleted.");
    }
    Ok(())
}
