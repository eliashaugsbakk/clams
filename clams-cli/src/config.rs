//! Configuration handling for the Clams content CLI.
//!
//! Disclaimer: Written by an LLM. This file implements XDG config discovery,
//! first-run prompts, interactive updates, persistence, and token input.

use dialoguer::{Input, Password};
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;

#[derive(Serialize, Deserialize, Debug)]
pub struct Config {
    pub server_url: String,
    pub auth_token: String,
}

impl Config {
    pub fn get_config_path() -> Result<PathBuf, Box<dyn std::error::Error>> {
        let config_dir =
            dirs::config_dir().ok_or("Could not find $XDG_CONFIG_HOME or home directory")?;

        let app_dir = config_dir.join("clams-cli");
        fs::create_dir_all(&app_dir)?;

        Ok(app_dir.join("config.toml"))
    }

    pub fn load() -> Result<Self, Box<dyn std::error::Error>> {
        let path = Self::get_config_path()?;

        if !path.exists() {
            println!(
                "No config found at {}. Configure the Clams server.",
                path.display()
            );
            let config = Config {
                server_url: Input::new().with_prompt("Server URL").interact_text()?,
                auth_token: Password::new().with_prompt("API token").interact()?,
            };
            config.save(&path)?;
            return Ok(config);
        }

        let contents = fs::read_to_string(&path)?;
        let config: Config = toml::from_str(&contents)?;
        Ok(config)
    }

    pub fn update_interactively(&self) -> Result<Self, Box<dyn std::error::Error>> {
        let server_url = Input::new()
            .with_prompt("Server URL")
            .default(self.server_url.clone())
            .interact_text()?;
        let entered_token = Password::new()
            .with_prompt("API token (leave blank to keep current)")
            .allow_empty_password(true)
            .interact()?;

        Ok(Self {
            server_url,
            auth_token: if entered_token.is_empty() {
                self.auth_token.clone()
            } else {
                entered_token
            },
        })
    }

    pub fn save(&self, path: &PathBuf) -> Result<(), Box<dyn std::error::Error>> {
        let contents = toml::to_string_pretty(self)?;
        fs::write(path, contents)?;
        Ok(())
    }
}
