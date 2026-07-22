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
            published TEXT NOT NULL,
            last_edited TEXT NOT NULL,
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

    try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {

      stmt.execute(posts);
      stmt.execute(images);

    } catch (SQLException e) {
      throw new RepoException("Error while initializing database", e);
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
