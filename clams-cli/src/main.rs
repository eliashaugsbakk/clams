//! Command-line entry point for managing Clams content.
//!
//! Disclaimer: This file was created and edited by an AI model.

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
    Images,
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
        (_, Some(Command::Images)) => {
            print_images(&client)?;
        }
        (_, Some(Command::Config)) => unreachable!("configuration is handled before API setup"),
        (None, None) => match Select::new()
            .with_prompt("What would you like to manage?")
            .items(&[
                "Blog post upload",
                "Blog post edit",
                "Blog post delete",
                "Image overview",
                "Add project",
                "Edit project",
                "Remove project",
                "Update configuration",
                "Exit",
            ])
            .default(0)
            .interact()?
        {
            0 => {
                let dir = dialoguer::Input::<String>::new()
                    .with_prompt("Blog directory")
                    .interact_text()?;
                blog::upload(&client, &dir, None)?;
            }
            1 => {
                let slug = Input::<String>::new()
                    .with_prompt("Post slug")
                    .interact_text()?;
                let dir = Input::<String>::new()
                    .with_prompt("Blog directory")
                    .interact_text()?;
                blog::upload(&client, &dir, Some(&slug))?;
            }
            2 => {
                let slug = Input::<String>::new()
                    .with_prompt("Post slug")
                    .interact_text()?;
                blog::delete(&client, &slug)?;
            }
            3 => print_images(&client)?,
            4 => projects::add(&client)?,
            5 => {
                let id = Input::<i64>::new()
                    .with_prompt("Project ID")
                    .interact_text()?;
                projects::edit(&client, id)?;
            }
            6 => {
                let id = Input::<i64>::new()
                    .with_prompt("Project ID")
                    .interact_text()?;
                projects::remove(&client, id)?;
            }
            7 => {
                update_configuration(&mut config)?;
            }
            _ => {}
        },
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
            "{}\t{}\t/media/{}",
            image.uuid, image.original_filename, image.uuid
        );
    }
    Ok(())
}
