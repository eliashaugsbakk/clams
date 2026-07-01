package no.eliashaugsbakk.clams.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostMetaData;

public class BlogPostRepoSqlite implements BlogPostRepo {
  private final SqliteManager manager;

  public BlogPostRepoSqlite(SqliteManager manager) {
    this.manager = manager;
  }

  @Override
  public List<PostMetaData> listPostsByPublishedDesc() {
    String sql = """
        SELECT title, slug, published, last_edited
        FROM posts
        ORDER BY published DESC
        """;

    List<PostMetaData> posts = new ArrayList<>();

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet resultSet = stmt.executeQuery()) {

      while (resultSet.next()) {
        String title = resultSet.getString("title");
        String slug = resultSet.getString("slug");

        Instant published = Instant.parse(resultSet.getString("published"));

        String lastEditRaw = resultSet.getString("last_edited");
        Instant lastEdit = (lastEditRaw != null) ? Instant.parse(lastEditRaw) : null;

        posts.add(new PostMetaData(title, slug, published, lastEdit));
      }

      return posts;

    } catch (SQLException e) {
      throw new RepoException("Error fetching all post metadata sorted by time", e);
    }
  }

  @Override
  public Optional<Post> getPost(String slug) {
    String sql = """
        SELECT title, slug, content, published
        FROM posts
        WHERE slug = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, slug);

      try (ResultSet resultSet = stmt.executeQuery()) {
        if (resultSet.next()) {
          String title = resultSet.getString("title");
          String postSlug = resultSet.getString("slug");
          String content = resultSet.getString("content");

          String publishedRaw = resultSet.getString("published");
          Instant timePublished = Instant.parse(publishedRaw);

          return Optional.of(new Post(title, postSlug, timePublished, content));
        }
        return Optional.empty();
      }

    } catch (SQLException e) {
      throw new RepoException("Error fetching full post by slug: " + slug, e);
    }
  }
}
