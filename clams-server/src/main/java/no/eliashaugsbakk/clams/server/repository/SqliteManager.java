package no.eliashaugsbakk.clams.server.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteManager implements AutoCloseable {
  private final HikariDataSource dataSource;

  public SqliteManager(String dbPath) {
    // HikariCP pools reusable connections across web threads, avoiding the overhead of opening/closing disk files on every request.
    HikariConfig config = new HikariConfig();

    config.setJdbcUrl("jdbc:sqlite:" + dbPath);
    config.setMaximumPoolSize(10);

    config.addDataSourceProperty("journal_mode", "WAL");
    config.addDataSourceProperty("busy_timeout", "5000");
    config.addDataSourceProperty("synchronous", "NORMAL");
    config.addDataSourceProperty("foreign_keys", "on");

    this.dataSource = new HikariDataSource(config);
  }

  public void init() {
    String posts = """
        CREATE TABLE IF NOT EXISTS posts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            slug TEXT NOT NULL UNIQUE,
            title TEXT NOT NULL,
            content TEXT,
            summary TEXT,
            created_at TEXT NOT NULL,
            published_at TEXT,
            updated_at TEXT NOT NULL,
            is_published BOOLEAN
        );
        """;

    String images = """
        CREATE TABLE IF NOT EXISTS images (
            uuid TEXT PRIMARY KEY,
            original_filename TEXT NOT NULL,
            extension TEXT,
            time_uploaded TEXT NOT NULL
        );
        """;

    String projects = """
        CREATE TABLE IF NOT EXISTS projects (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            read_more_url TEXT,
            git_url TEXT,
            git_hub_url TEXT,
            description TEXT,
            display_order INTEGER NOT NULL DEFAULT 0
        );
        """;

    try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {

      stmt.execute(posts);
      migratePostsSchema(conn);
      stmt.execute(images);
      stmt.execute(projects);
      migrateProjectsSchema(conn);

    } catch (SQLException e) {
      throw new RepoException("Error while initializing database", e);
    }
  }

  // BEGIN LLM EDIT: Migrate deployed databases to the current post and project schemas.
  /**
   * Disclaimer: These migrations were written by an LLM to keep existing SQLite installations
   * compatible with the current post timestamp and project ordering schemas.
   */
  private void migratePostsSchema(Connection conn) throws SQLException {
    boolean legacyTimestamps = hasColumn(conn, "posts", "published")
        && hasColumn(conn, "posts", "last_edited");
    boolean needsRebuild = legacyTimestamps
        || !hasColumn(conn, "posts", "created_at")
        || isNotNull(conn, "posts", "published_at");

    if (needsRebuild) {
      try (Statement stmt = conn.createStatement()) {
        String createdAt = hasColumn(conn, "posts", "created_at") ? "created_at" : "published";
        String publishedAt = legacyTimestamps ? "published" : "published_at";
        String updatedAt = legacyTimestamps ? "last_edited" : "updated_at";
        stmt.execute("""
            CREATE TABLE posts_migrated (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                slug TEXT NOT NULL UNIQUE,
                title TEXT NOT NULL,
                content TEXT,
                summary TEXT,
                created_at TEXT NOT NULL,
                published_at TEXT,
                updated_at TEXT NOT NULL,
                is_published BOOLEAN
            )
            """);
        stmt.execute("INSERT INTO posts_migrated "
            + "(id, slug, title, content, summary, created_at, published_at, updated_at, is_published) "
            + "SELECT id, slug, title, content, summary, " + createdAt + ", " + publishedAt + ", "
            + updatedAt + ", is_published FROM posts");
        stmt.execute("DROP TABLE posts");
        stmt.execute("ALTER TABLE posts_migrated RENAME TO posts");
      }
    }
  }

  private void migrateProjectsSchema(Connection conn) throws SQLException {
    if (!hasColumn(conn, "projects", "display_order")) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("ALTER TABLE projects ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0");
      }
    }
  }

  private boolean isNotNull(Connection conn, String table, String column) throws SQLException {
    try (var stmt = conn.createStatement();
        var columns = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
      while (columns.next()) {
        if (column.equals(columns.getString("name"))) {
          return columns.getBoolean("notnull");
        }
      }
      return false;
    }
  }

  private boolean hasColumn(Connection conn, String table, String column) throws SQLException {
    try (var stmt = conn.createStatement();
        var columns = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
      while (columns.next()) {
        if (column.equals(columns.getString("name"))) {
          return true;
        }
      }
      return false;
    }
  }
  // END LLM EDIT

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Override
  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }
}
