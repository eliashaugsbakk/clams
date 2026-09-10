//! Interactive project CRUD workflows.
//!
//! Disclaimer: Written by an LLM. This file prompts for project fields
//! and performs project create, update, and delete calls.

use crate::client::{ApiClient, Project};
use dialoguer::Input;

fn prompt(id: Option<i64>) -> Result<Project, Box<dyn std::error::Error>> {
    Ok(Project {
        id,
        name: Input::new().with_prompt("Name").interact_text()?,
        read_more_url: Input::new()
            .with_prompt("Read more URL")
            .allow_empty(true)
            .interact_text()?,
        git_url: Input::new()
            .with_prompt("Git URL")
            .allow_empty(true)
            .interact_text()?,
        github_url: Input::new()
            .with_prompt("GitHub URL")
            .allow_empty(true)
            .interact_text()?,
        description: Input::new().with_prompt("Description").interact_text()?,
    })
}

pub fn add(client: &ApiClient) -> Result<(), Box<dyn std::error::Error>> {
    client.create_project(&prompt(None)?)?;
    println!("Project created.");
    Ok(())
}

pub fn edit(client: &ApiClient, id: i64) -> Result<(), Box<dyn std::error::Error>> {
    client.update_project(id, &prompt(Some(id))?)?;
    println!("Project updated.");
    Ok(())
}

pub fn remove(client: &ApiClient, id: i64) -> Result<(), Box<dyn std::error::Error>> {
    client.delete_project(id)?;
    println!("Project removed.");
    Ok(())
}
