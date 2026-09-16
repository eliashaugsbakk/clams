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
            content TEXT NOT NULL,
            summary TEXT,
            created_at TEXT NOT NULL,
            published_at TEXT,
            updated_at TEXT NOT NULL,
            is_published BOOLEAN NOT NULL
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
      stmt.execute(images);
      stmt.execute(projects);
      validateSchema(conn);

    } catch (SQLException e) {
      throw new RepoException("Error while initializing database", e);
    }
  }

  private void validateSchema(Connection conn) throws SQLException {
    validateTable(conn, "posts", new String[] {
        "id", "slug", "title", "content", "summary", "created_at",
        "published_at", "updated_at", "is_published"
    });
    validateNotNull(conn, "posts", "content");
    validateNotNull(conn, "posts", "is_published");
    validateTable(conn, "images", new String[] {
        "uuid", "original_filename", "extension", "time_uploaded"
    });
    validateTable(conn, "projects", new String[] {
        "id", "name", "read_more_url", "git_url", "git_hub_url", "description", "display_order"
    });
  }

  private void validateNotNull(Connection conn, String table, String column) throws SQLException {
    try (var stmt = conn.createStatement();
        var columns = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
      while (columns.next()) {
        if (column.equals(columns.getString("name")) && columns.getBoolean("notnull")) {
          return;
        }
      }
      throw new SQLException("Database schema for table '" + table
          + "' requires column '" + column + "' to be NOT NULL.");
    }
  }

  private void validateTable(Connection conn, String table, String[] requiredColumns)
      throws SQLException {
    try (var stmt = conn.createStatement();
        var columns = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
      var found = new java.util.HashSet<String>();
      while (columns.next()) {
        found.add(columns.getString("name"));
      }
      for (String column : requiredColumns) {
        if (!found.contains(column)) {
          throw new SQLException("Database schema for table '" + table
              + "' is missing required column '" + column + "'.");
        }
      }
    }
  }

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
