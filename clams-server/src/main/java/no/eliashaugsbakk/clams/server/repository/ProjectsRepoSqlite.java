package no.eliashaugsbakk.clams.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import no.eliashaugsbakk.clams.server.model.Project;

public class ProjectsRepoSqlite implements ProjectsRepo {
  private final SqliteManager manager;

  public ProjectsRepoSqlite(SqliteManager manager) {
    this.manager = manager;
  }

  @Override
  public void addProject(Project project) {
    String sql = """
        INSERT INTO projects (name, read_more_url, git_url, git_hub_url, description, display_order)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    String nextOrderSql = "SELECT COALESCE(MAX(display_order), -1) + 1 FROM projects";

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        PreparedStatement nextOrderStmt = conn.prepareStatement(nextOrderSql)) {

      stmt.setString(1, project.name());
      stmt.setString(2, project.readMoreUrl());
      stmt.setString(3, project.gitUrl());
      stmt.setString(4, project.gitHubUrl());
      stmt.setString(5, project.description());
      int displayOrder;
      if (project.displayOrder() != null) {
        displayOrder = project.displayOrder();
      } else {
        try (ResultSet resultSet = nextOrderStmt.executeQuery()) {
          resultSet.next();
          displayOrder = resultSet.getInt(1);
        }
      }
      stmt.setInt(6, displayOrder);

      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new RepoException("Error adding project: " + project.name(), e);
    }
  }

  @Override
  public boolean updateProject(Project project) {
    String sql = """
        UPDATE projects
        SET name = ?, read_more_url = ?, git_url = ?, git_hub_url = ?, description = ?
            , display_order = COALESCE(?, display_order)
        WHERE id = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, project.name());
      stmt.setString(2, project.readMoreUrl());
      stmt.setString(3, project.gitUrl());
      stmt.setString(4, project.gitHubUrl());
      stmt.setString(5, project.description());
      stmt.setObject(6, project.displayOrder());
      stmt.setLong(7, project.id());

      int rowsUpdated = stmt.executeUpdate();
      return rowsUpdated > 0;
    } catch (SQLException e) {
      throw new RepoException("Error updating project: ", e);
    }
  }

  @Override
  public boolean deleteProject(long id) {
    String sql = """
        DELETE FROM projects
        WHERE id = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setLong(1, id);

      int rowsUpdated = stmt.executeUpdate();
      return rowsUpdated > 0;
    } catch (SQLException e) {
      throw new RepoException("Error deleting project " + id + ": ", e);
    }
  }

  @Override
  public List<Project> getAllProjects() {
    String sql = """
        SELECT id, name, read_more_url, git_url, git_hub_url, description, display_order
        FROM projects
        ORDER BY display_order ASC, id ASC
        """;

    List<Project> projects = new ArrayList<>();

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet resultSet = stmt.executeQuery()) {

      while (resultSet.next()) {
        long id = resultSet.getLong("id");
        String name = resultSet.getString("name");
        String read_more_url = resultSet.getString("read_more_url");
        String git_url = resultSet.getString("git_url");
        String git_hub_url = resultSet.getString("git_hub_url");
        String description = resultSet.getString("description");
        int displayOrder = resultSet.getInt("display_order");

        projects.add(new Project(id, name, read_more_url, git_url, git_hub_url, description,
            displayOrder));
      }

      return projects;
    } catch (SQLException e) {
      throw new RepoException("Error fetching all projects", e);
    }
  }
}
