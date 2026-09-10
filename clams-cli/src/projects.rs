//! Interactive project CRUD workflows.
//!
//! Disclaimer: Written by an LLM. This file prompts for project fields
//! and performs project create, update, and delete calls.

use crate::client::{ApiClient, Project};
use dialoguer::{Confirm, Input, Select};

fn prompt(
    id: Option<i64>,
    existing: Option<&Project>,
) -> Result<Project, Box<dyn std::error::Error>> {
    Ok(Project {
        id,
        name: Input::new()
            .with_prompt("Name")
            .with_initial_text(existing.map(|p| p.name.clone()).unwrap_or_default())
            .interact_text()?,
        read_more_url: Input::new()
            .with_prompt("Read more URL")
            .with_initial_text(
                existing
                    .map(|p| p.read_more_url.clone())
                    .unwrap_or_default(),
            )
            .allow_empty(true)
            .interact_text()?,
        git_url: Input::new()
            .with_prompt("Git URL")
            .with_initial_text(existing.map(|p| p.git_url.clone()).unwrap_or_default())
            .allow_empty(true)
            .interact_text()?,
        github_url: Input::new()
            .with_prompt("GitHub URL")
            .with_initial_text(existing.map(|p| p.github_url.clone()).unwrap_or_default())
            .allow_empty(true)
            .interact_text()?,
        description: Input::new()
            .with_prompt("Description")
            .with_initial_text(existing.map(|p| p.description.clone()).unwrap_or_default())
            .interact_text()?,
    })
}

pub fn add(client: &ApiClient) -> Result<(), Box<dyn std::error::Error>> {
    client.create_project(&prompt(None, None)?)?;
    println!("Project created.");
    Ok(())
}

pub fn edit(client: &ApiClient, id: i64) -> Result<(), Box<dyn std::error::Error>> {
    let existing = client
        .list_projects()?
        .into_iter()
        .find(|project| project.id == Some(id))
        .ok_or_else(|| format!("Project with ID {id} was not found"))?;
    client.update_project(id, &prompt(Some(id), Some(&existing))?)?;
    println!("Project updated.");
    Ok(())
}

pub fn remove(client: &ApiClient, id: i64) -> Result<(), Box<dyn std::error::Error>> {
    let existing = client
        .list_projects()?
        .into_iter()
        .find(|project| project.id == Some(id))
        .ok_or_else(|| format!("Project with ID {id} was not found"))?;
    remove_project(client, &existing)?;
    Ok(())
}

pub fn edit_selected(client: &ApiClient) -> Result<(), Box<dyn std::error::Error>> {
    let Some(existing) = select_project(client, "Select a project to edit")? else {
        return Ok(());
    };
    client.update_project(
        existing.id.ok_or("Selected project has no ID")?,
        &prompt(existing.id, Some(&existing))?,
    )?;
    println!("Project updated.");
    Ok(())
}

pub fn remove_selected(client: &ApiClient) -> Result<(), Box<dyn std::error::Error>> {
    let Some(existing) = select_project(client, "Select a project to remove")? else {
        return Ok(());
    };
    remove_project(client, &existing)?;
    Ok(())
}

fn select_project(
    client: &ApiClient,
    prompt_text: &str,
) -> Result<Option<Project>, Box<dyn std::error::Error>> {
    let projects = client.list_projects()?;
    if projects.is_empty() {
        println!("No projects found.");
        return Ok(None);
    }
    let labels: Vec<String> = projects
        .iter()
        .map(|project| {
            format!(
                "{} (ID: {})",
                project.name,
                project
                    .id
                    .map_or_else(|| "unknown".to_string(), |id| id.to_string())
            )
        })
        .collect();
    let index = Select::new()
        .with_prompt(prompt_text)
        .items(&labels)
        .interact()?;
    Ok(Some(projects[index].clone()))
}

fn remove_project(client: &ApiClient, project: &Project) -> Result<(), Box<dyn std::error::Error>> {
    let id = project.id.ok_or("Selected project has no ID")?;
    if Confirm::new()
        .with_prompt(format!("Remove project '{}' (ID {id})?", project.name))
        .default(false)
        .interact()?
    {
        client.delete_project(id)?;
        println!("Project removed.");
    }
    Ok(())
}
