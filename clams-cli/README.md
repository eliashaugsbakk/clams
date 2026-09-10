# clams-cli

> [!NOTE]
> The Rust client in this directory was created entirely through AI-assisted
> prompting (vibe coding). The server is separate. Read and understand the
> client code before using it, especially against production systems or
> content. Take appropriate precautions, test changes safely, and keep backups.

`clams-cli` is a Linux-oriented Rust command-line client for the Clams CMS.
It uploads and edits blog posts, manages projects, and lists reusable images.

## Build and configure

Build the release binary:

```sh
cargo build --release
```

On first use, the CLI automatically creates its configuration directory and
prompts for the server URL and API token:

```sh
clams-cli
```

The configuration is stored at:

```text
~/.config/clams-cli/config.toml
```

Update it interactively at any time:

```sh
clams-cli config
```

The command saves new values before testing the connection. If the test fails,
it keeps the saved values and lets you retry the prompts or leave them as-is.
Advanced users may also edit the file manually:

```toml
server_url = "https://example.com"
auth_token = "the-server-authorization-token"
```

The token is sent as an `Authorization: Bearer ...` header for authenticated
API calls. Keep the file private because it contains the API token.

## Commands

```sh
clams-cli ./content/my-post
clams-cli blog upload ./content/my-post
clams-cli blog edit existing-slug ./content/my-post
clams-cli blog delete existing-slug
clams-cli project add
clams-cli project edit 12
clams-cli project remove 12
clams-cli images
clams-cli config
```

A blog directory must contain exactly one `.md` file and may contain `.jpeg`
images. Images are validated against the server's 2000x2000 pixel limit,
uploaded individually, and their filename references are replaced in the
Markdown with public `/media/<uuid>` URLs. The Markdown file is updated locally
after each successful upload.

Running `clams-cli` without arguments provides the common operations through an
interactive menu organized into Blog posts, Projects, Images, and
Configuration sections. Project edit and removal show a list of projects and
let you select one by name instead of requiring you to look up its numeric ID.
`-h` and `--help` show the command reference.

## Source layout

- `main.rs` defines the hybrid positional/subcommand interface.
- `config.rs` loads and updates the Linux XDG configuration.
- `client.rs` contains authenticated HTTP and API data types.
- `file_io.rs` validates and discovers blog package files.
- `blog.rs` implements post upload, edit, and deletion.
- `projects.rs` implements interactive project CRUD.

The server exposes public image retrieval at `/media/{uuid}`. Mutations and
metadata listing remain under authenticated `/api` routes. Post listing and
retrieval are also available under `/api/posts` and `/api/posts/{slug}`.
