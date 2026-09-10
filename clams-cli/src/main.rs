//! Command-line entry point for managing Clams content.
//!
//! Disclaimer: This file was created and edited by an AI model.

mod blog;
mod client;
mod config;
mod file_io;
mod projects;

use clap::{Parser, Subcommand};

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
    let config = config::Config::load()?;
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
            for image in client.list_images()? {
                println!(
                    "{}\t{}\t/media/{}",
                    image.uuid, image.original_filename, image.uuid
                );
            }
        }
        (None, None) => println!("Use --help for commands."),
    }
    Ok(())
}
