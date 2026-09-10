//! Command-line entry point for managing Clams content.
//!
//! Disclaimer: Written by an LLM. This file defines the CLI parser, interactive menu,
//! configuration-update flow, and dispatch for blog, project, and image operations.

mod blog;
mod client;
mod config;
mod file_io;
mod projects;

use clap::{Parser, Subcommand};
use dialoguer::{Confirm, Input, Select};

#[derive(Parser)]
#[command(
    name = "clams-cli",
    about = "Manage Clams blog posts, projects, and images"
)]
struct Cli {
    #[arg(value_name = "BLOG_DIR")]
    blog_dir: Option<String>,
    #[command(subcommand)]
    command: Option<Command>,
}

#[derive(Subcommand)]
enum Command {
    Blog {
        #[command(subcommand)]
        action: BlogAction,
    },
    Project {
        #[command(subcommand)]
        action: ProjectAction,
    },
    Images {
        #[command(subcommand)]
        action: Option<ImagesAction>,
    },
    Config,
}

#[derive(Subcommand)]
enum BlogAction {
    Upload { dir: String },
    Edit { slug: String, dir: String },
    Delete { slug: String },
}

#[derive(Subcommand)]
enum ProjectAction {
    Add,
    Edit { id: i64 },
    Remove { id: i64 },
}

#[derive(Subcommand)]
enum ImagesAction {
    Upload { path: String },
    UploadDir { directory: String },
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();
    let mut config = config::Config::load()?;

    if matches!(&cli.command, Some(Command::Config)) {
        update_configuration(&mut config)?;
        return Ok(());
    }

    let client = client::ApiClient::new(&config)?;

    match (cli.blog_dir, cli.command) {
        (Some(dir), None) => blog::upload(&client, &dir, None)?,
        (_, Some(Command::Blog { action })) => match action {
            BlogAction::Upload { dir } => blog::upload(&client, &dir, None)?,
            BlogAction::Edit { slug, dir } => blog::upload(&client, &dir, Some(&slug))?,
            BlogAction::Delete { slug } => blog::delete(&client, &slug)?,
        },
        (_, Some(Command::Project { action })) => match action {
            ProjectAction::Add => projects::add(&client)?,
            ProjectAction::Edit { id } => projects::edit(&client, id)?,
            ProjectAction::Remove { id } => projects::remove(&client, id)?,
        },
        (_, Some(Command::Images { action })) => match action {
            Some(ImagesAction::Upload { path }) => upload_image(&client, &path)?,
            Some(ImagesAction::UploadDir { directory }) => {
                upload_image_directory(&client, &directory)?
            }
            None => print_images(&client)?,
        },
        (_, Some(Command::Config)) => unreachable!("configuration is handled before API setup"),
        (None, None) => interactive_menu(&client, &mut config)?,
    }
    Ok(())
}

fn update_configuration(config: &mut config::Config) -> Result<(), Box<dyn std::error::Error>> {
    loop {
        let updated = config.update_interactively()?;
        updated.save(&config::Config::get_config_path()?)?;
        *config = updated;
        match client::ApiClient::new(config)?.test_connection() {
            Ok(()) => {
                println!("Configuration saved and connection verified.");
                break;
            }
            Err(error) => {
                eprintln!("Configuration saved, but the connection test failed: {error}");
                if !Confirm::new()
                    .with_prompt("Try entering the configuration again?")
                    .default(false)
                    .interact()?
                {
                    break;
                }
            }
        }
    }
    Ok(())
}

fn print_images(client: &client::ApiClient) -> Result<(), Box<dyn std::error::Error>> {
    for image in client.list_images()? {
        println!(
            "{}\t{}\t{}\t{}\t/media/{}",
            image.uuid,
            image.original_filename.as_deref().unwrap_or("(unnamed)"),
            image.content_type.as_deref().unwrap_or("(unknown type)"),
            image.time_uploaded.as_deref().unwrap_or("(unknown time)"),
            image.uuid
        );
    }
    Ok(())
}

fn interactive_menu(
    client: &client::ApiClient,
    config: &mut config::Config,
) -> Result<(), Box<dyn std::error::Error>> {
    loop {
        match Select::new()
            .with_prompt("What would you like to manage?")
            .items(&["Blog posts", "Projects", "Images", "Configuration", "Exit"])
            .default(0)
            .interact()?
        {
            0 => blog_menu(client)?,
            1 => project_menu(client)?,
            2 => images_menu(client)?,
            3 => update_configuration(config)?,
            _ => break,
        }
    }
    Ok(())
}

fn blog_menu(client: &client::ApiClient) -> Result<(), Box<dyn std::error::Error>> {
    match Select::new()
        .with_prompt("Blog posts")
        .items(&["Upload post", "Edit post", "Delete post", "Back"])
        .default(0)
        .interact()?
    {
        0 => {
            let dir = Input::<String>::new()
                .with_prompt("Blog directory")
                .interact_text()?;
            blog::upload(client, &dir, None)?;
        }
        1 => {
            let slug = Input::<String>::new()
                .with_prompt("Post slug")
                .interact_text()?;
            let dir = Input::<String>::new()
                .with_prompt("Blog directory")
                .interact_text()?;
            blog::upload(client, &dir, Some(&slug))?;
        }
        2 => {
            let slug = Input::<String>::new()
                .with_prompt("Post slug")
                .interact_text()?;
            blog::delete(client, &slug)?;
        }
        _ => {}
    }
    Ok(())
}

fn project_menu(client: &client::ApiClient) -> Result<(), Box<dyn std::error::Error>> {
    match Select::new()
        .with_prompt("Projects")
        .items(&["Add project", "Edit project", "Remove project", "Back"])
        .default(0)
        .interact()?
    {
        0 => projects::add(client)?,
        1 => projects::edit_selected(client)?,
        2 => projects::remove_selected(client)?,
        _ => {}
    }
    Ok(())
}

fn images_menu(client: &client::ApiClient) -> Result<(), Box<dyn std::error::Error>> {
    match Select::new()
        .with_prompt("Images")
        .items(&[
            "Upload image",
            "Upload image directory",
            "List images",
            "Back",
        ])
        .default(0)
        .interact()?
    {
        0 => {
            let path = Input::<String>::new()
                .with_prompt("JPEG image path")
                .interact_text()?;
            upload_image(client, &path)?;
        }
        1 => {
            let directory = Input::<String>::new()
                .with_prompt("JPEG image directory")
                .interact_text()?;
            upload_image_directory(client, &directory)?;
        }
        2 => print_images(client)?,
        _ => {}
    }
    Ok(())
}

fn upload_image(client: &client::ApiClient, path: &str) -> Result<(), Box<dyn std::error::Error>> {
    let path = std::path::Path::new(path);
    file_io::validate_image(path)?;
    let upload = client.upload_image(path)?;
    println!("Image uploaded successfully.");
    println!("Filename: {}", upload.original_filename);
    println!("UUID: {}", upload.uuid);
    println!(
        "Content type: {}",
        upload.content_type.as_deref().unwrap_or("(unknown)")
    );
    println!("Uploaded at: {}", upload.time_uploaded);
    println!("URL: {}", upload.url);
    Ok(())
}

fn upload_image_directory(
    client: &client::ApiClient,
    directory: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    let images = file_io::collect_images(directory)?;
    println!("Validated {} image(s). Uploading...", images.len());

    let mut uploaded = 0;
    for image in images {
        upload_image(client, &image.to_string_lossy())?;
        uploaded += 1;
    }
    println!("Uploaded {uploaded} image(s).");
    Ok(())
}
