# clams-cli

> Disclaimer: This CLI was developed by an AI model. Review and adapt it before
> using it against production content.

`clams-cli` is a Linux-oriented Rust command-line client for the Clams CMS.
It uploads and edits blog posts, manages projects, and lists reusable images.

## Build and configure

```sh
cargo build --release
mkdir -p ~/.config/clams-cli
chmod 700 ~/.config/clams-cli
```

Create `~/.config/clams-cli/config.toml`:

```toml
server_url = "https://example.com"
auth_token = "the-server-authorization-token"
```

The token is sent as `Authorization: Bearer ...` for authenticated API calls.

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
```

A blog directory must contain exactly one `.md` file and may contain `.jpeg`
images. Images are validated against the server's 2000x2000 pixel limit,
uploaded individually, and their filename references are replaced in the
Markdown with public `/media/<uuid>` URLs. The Markdown file is updated locally
after each successful upload.

## Source layout

- `main.rs` defines the hybrid positional/subcommand interface.
- `config.rs` loads the Linux XDG configuration.
- `client.rs` contains authenticated HTTP and API data types.
- `file_io.rs` validates and discovers blog package files.
- `blog.rs` implements post upload, edit, and deletion.
- `projects.rs` implements interactive project CRUD.

The server exposes public image retrieval at `/media/{uuid}`. Mutations and
metadata listing remain under authenticated `/api` routes. Post listing and
retrieval are also available under `/api/posts` and `/api/posts/{slug}`.
