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
        INSERT INTO projects (name, read_more_url, git_url, git_hub_url, description)
        VALUES (?, ?, ?, ?, ?)
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, project.name());
      stmt.setString(2, project.readMoreUrl());
      stmt.setString(3, project.gitUrl());
      stmt.setString(4, project.gitHubUrl());
      stmt.setString(5, project.description());

      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new RepoException("Error adding project: " + project.name(), e);
    }
  }

  @Override
  public void updateProject(long id, Project project) {
    String sql = """
        UPDATE projects
        SET name = ?, read_more_url = ?, git_url = ?, git_hub_url = ?, description = ?
        WHERE id = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, project.name());
      stmt.setString(2, project.readMoreUrl());
      stmt.setString(3, project.gitUrl());
      stmt.setString(4, project.gitHubUrl());
      stmt.setString(5, project.description());
      stmt.setLong(6, id);

      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new RepoException("Error updating project: ", e);
    }
  }

  @Override
  public void deleteProject(long id) {
    String sql = """
        DELETE FROM projects
        WHERE id = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setLong(1, id);

      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RepoException("Error deleting project " + id + ": ", e);
    }
  }

  @Override
  public List<Project> getAllProjects() {
    String sql = """
        SELECT id, name, read_more_url, git_url, git_hub_url, description
        FROM projects
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

        projects.add(new Project(id, name, read_more_url, git_url, git_hub_url, description));
      }

      return projects;
    } catch (SQLException e) {
      throw new RepoException("Error fetching all projects", e);
    }
  }
}
