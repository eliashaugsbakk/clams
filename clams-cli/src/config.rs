//! Configuration handling for the Clams content CLI.
//!
//! Disclaimer: This file was created and edited by an AI model.

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
            return Err(format!(
                "No config found at {}. Create it with server_url and auth_token.",
                path.display()
            )
            .into());
        }

        let contents = fs::read_to_string(&path)?;
        let config: Config = toml::from_str(&contents)?;
        Ok(config)
    }

    pub fn save(&self, path: &PathBuf) -> Result<(), Box<dyn std::error::Error>> {
        let contents = toml::to_string_pretty(self)?;
        fs::write(path, contents)?;
        Ok(())
    }
}
