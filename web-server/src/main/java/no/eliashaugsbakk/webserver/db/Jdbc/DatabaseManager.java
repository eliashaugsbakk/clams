package no.eliashaugsbakk.webserver.db.Jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import no.eliashaugsbakk.webserver.Config;

public class DatabaseManager {

  private final String url;

  public DatabaseManager() {
    String dbFilePath = Config.getInstance().getDbPath();
    this.url = "jdbc:sqlite:" + dbFilePath + "?busy_timeout=5000";
  }

  public void initialize() throws SQLException, IOException {
    String posts = """
            CREATE TABLE IF NOT EXISTS posts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                slug TEXT NOT NULL UNIQUE,
                title TEXT NOT NULL,
                content_raw TEXT,
                content_html TEXT,
                summary TEXT,
                published_at TEXT,
                last_edited_at TEXT,
                is_published BOOLEAN
            );
        """;

    String tags = """
            CREATE TABLE IF NOT EXISTS tags (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT NOT NULL UNIQUE
            );
        """;

    String post_tags = """
            CREATE TABLE IF NOT EXISTS post_tags (
                post_id INTEGER,
                tag_id INTEGER,
                PRIMARY KEY (post_id, tag_id),
                FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
            );
        """;

    String tokens = """
            CREATE TABLE IF NOT EXISTS tokens (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tokenValue TEXT NOT NULL UNIQUE,
                createdAt TEXT
            );
        """;

    try (Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement()) {

      stmt.execute("PRAGMA foreign_keys = ON;");

      stmt.execute(posts);
      stmt.execute(tags);
      stmt.execute(post_tags);
      stmt.execute(tokens);

      IO.println("Database initialized successfully.");
    }
  }

  // New DB connection for every request to handle multiple concurrent requests
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(url);
  }
}
