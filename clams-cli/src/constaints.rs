
//! Legacy CLI help text retained for compatibility.
//!
//! Disclaimer: Written by an LLM. This legacy help text is retained for reference
//! but is no longer used by the Clap-based command dispatcher.

pub const HELP_MESSAGE: &str = "\
clams-cli 0.1.0
Content management CLI tool for blog posts and projects

USAGE:
    clams-cli <--blog|--project> <ACTION> [ARGUMENTS...]

RESOURCES:
    --blog       Manage blog posts
    --project    Manage projects

ACTIONS FOR BLOGS:
    add <dir>                   Upload images from directory, process markdown, and create post
    update <slug> <dir>         Update an existing blog post specified by its slug
    remove <slug>               Delete a blog post specified by its slug

ACTIONS FOR PROJECTS:
    add <dir>                   Upload project assets and create a new project entry
    update <id> <dir>           Update an existing project specified by its ID
    remove <id>                 Delete a project specified by its ID

EXAMPLES:
    clams-cli --blog add ./content/my-first-post/
    clams-cli --blog update my-first-post ./content/my-first-post/
    clams-cli --blog remove my-first-post
    clams-cli --project add ./content/my-project/

OPTIONS:
    -h, --help                  Print help information
    -V, --version               Print version information
";
