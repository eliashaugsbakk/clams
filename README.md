# Clams CMS

> [!NOTE]
> This project has been developed in stages. The Java server core was coded
> manually by Elias Haugsbakk. Later server functionality, the Rust CLI, and
> much of the supporting documentation were developed entirely with AI
> assistance. AI-generated code and documentation should be
> reviewed before production use, especially around authentication, storage,
> and content management.

Clams is a small personal blog CMS with a Java/Javalin server and a
Linux-oriented Rust command-line client. It stores posts, projects, and image
metadata in SQLite and stores uploaded JPEG files in the configured storage
directory.

## Repository layout

- `clams-server/` contains the Java HTTP server and browser-facing pages.
- `clams-cli/` contains the Rust client for managing posts, projects, and
  images.
- `docs/api.md` documents the authenticated HTTP API.

## Build

Build the Java server and its shaded JAR:

```sh
mvn clean package
```

Build the CLI:

```sh
cargo build --manifest-path clams-cli/Cargo.toml --release
```

The CLI configuration is stored at
`~/.config/clams-cli/config.toml`. The server configuration is stored at
`~/.config/clams/clams.properties`.

See [`clams-cli/README.md`](clams-cli/README.md) for CLI usage and
[`docs/api.md`](docs/api.md) for the HTTP API contract.
