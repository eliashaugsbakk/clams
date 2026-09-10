//! Authenticated HTTP client for the Clams server API.
//!
//! Disclaimer: This file was created and edited by an AI model.

use crate::config::Config;
use reqwest::blocking::{Client, multipart};
use serde::{Deserialize, Serialize, de::DeserializeOwned};
use std::{fs, path::Path};

#[derive(Debug, Deserialize)]
pub struct ImageResponse {
    pub uuid: String,
    pub original_filename: String,
    pub content_type: String,
    pub time_uploaded: String,
}

#[derive(Debug, Deserialize)]
pub struct UploadResponse {
    pub uuid: String,
    pub url: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Post {
    pub title: String,
    pub slug: String,
    pub summary: Option<String>,
    pub content: String,
    #[serde(rename = "isPublished")]
    pub is_published: bool,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Project {
    pub id: Option<i64>,
    pub name: String,
    #[serde(rename = "readMoreUrl")]
    pub read_more_url: String,
    #[serde(rename = "gitUrl")]
    pub git_url: String,
    #[serde(rename = "gitHubUrl")]
    pub github_url: String,
    pub description: String,
}

#[derive(Debug, Serialize)]
pub struct PostPayload {
    pub title: String,
    pub summary: String,
    pub content: String,
    #[serde(rename = "isPublished")]
    pub is_published: bool,
}

pub struct ApiClient {
    base_url: String,
    http: Client,
    token: String,
}

impl ApiClient {
    pub fn new(config: &Config) -> Result<Self, Box<dyn std::error::Error>> {
        Ok(Self {
            base_url: config.server_url.trim_end_matches('/').to_string(),
            http: Client::builder().build()?,
            token: config.auth_token.clone(),
        })
    }

    fn request(
        &self,
        request: reqwest::blocking::RequestBuilder,
    ) -> reqwest::blocking::RequestBuilder {
        request.bearer_auth(&self.token)
    }

    fn json<T: DeserializeOwned>(
        &self,
        response: reqwest::blocking::Response,
    ) -> Result<T, Box<dyn std::error::Error>> {
        let response = response.error_for_status()?;
        Ok(response.json()?)
    }

    pub fn upload_image(&self, path: &Path) -> Result<UploadResponse, Box<dyn std::error::Error>> {
        let bytes = fs::read(path)?;
        let part = multipart::Part::bytes(bytes)
            .file_name(
                path.file_name()
                    .ok_or("Image has no filename")?
                    .to_string_lossy()
                    .to_string(),
            )
            .mime_str("image/jpeg")?;
        let form = multipart::Form::new().part("image", part);
        let response = self
            .request(
                self.http
                    .post(format!("{}/api/media", self.base_url))
                    .multipart(form),
            )
            .send()?;
        self.json(response)
    }

    pub fn list_images(&self) -> Result<Vec<ImageResponse>, Box<dyn std::error::Error>> {
        let response = self
            .request(self.http.get(format!("{}/api/media", self.base_url)))
            .send()?;
        self.json(response)
    }

    pub fn list_posts(&self) -> Result<Vec<Post>, Box<dyn std::error::Error>> {
        let response = self
            .request(self.http.get(format!("{}/api/posts", self.base_url)))
            .send()?;
        self.json(response)
    }

    pub fn get_post(&self, slug: &str) -> Result<Post, Box<dyn std::error::Error>> {
        let response = self
            .request(self.http.get(format!("{}/api/posts/{slug}", self.base_url)))
            .send()?;
        self.json(response)
    }

    pub fn create_post(&self, payload: &PostPayload) -> Result<(), Box<dyn std::error::Error>> {
        self.request(
            self.http
                .post(format!("{}/api/posts", self.base_url))
                .json(payload),
        )
        .send()?
        .error_for_status()?;
        Ok(())
    }

    pub fn update_post(
        &self,
        slug: &str,
        payload: &PostPayload,
    ) -> Result<(), Box<dyn std::error::Error>> {
        self.request(
            self.http
                .put(format!("{}/api/posts/{slug}", self.base_url))
                .json(payload),
        )
        .send()?
        .error_for_status()?;
        Ok(())
    }

    pub fn delete_post(&self, slug: &str) -> Result<(), Box<dyn std::error::Error>> {
        self.request(
            self.http
                .delete(format!("{}/api/posts/{slug}", self.base_url)),
        )
        .send()?
        .error_for_status()?;
        Ok(())
    }

    pub fn list_projects(&self) -> Result<Vec<Project>, Box<dyn std::error::Error>> {
        let response = self
            .request(self.http.get(format!("{}/api/projects", self.base_url)))
            .send()?;
        self.json(response)
    }

    pub fn create_project(&self, project: &Project) -> Result<(), Box<dyn std::error::Error>> {
        self.request(
            self.http
                .post(format!("{}/api/projects", self.base_url))
                .json(project),
        )
        .send()?
        .error_for_status()?;
        Ok(())
    }

    pub fn update_project(
        &self,
        id: i64,
        project: &Project,
    ) -> Result<(), Box<dyn std::error::Error>> {
        self.request(
            self.http
                .put(format!("{}/api/projects/{id}", self.base_url))
                .json(project),
        )
        .send()?
        .error_for_status()?;
        Ok(())
    }

    pub fn delete_project(&self, id: i64) -> Result<(), Box<dyn std::error::Error>> {
        self.request(
            self.http
                .delete(format!("{}/api/projects/{id}", self.base_url)),
        )
        .send()?
        .error_for_status()?;
        Ok(())
    }
}
