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

public class PostsRepoSqlite implements PostsRepo {
  private final SqliteManager manager;

  public PostsRepoSqlite(SqliteManager manager) {
    this.manager = manager;
  }

  @Override
  public List<PostMetaData> listPostsMetaData() {
    String sql = """
        SELECT title, slug, summary, published, last_edited, is_published
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
        String summary = resultSet.getString("summary");
        Instant published = Instant.parse(resultSet.getString("published"));
        String lastEditRaw = resultSet.getString("last_edited");
        Instant lastEdit = (lastEditRaw != null) ? Instant.parse(lastEditRaw) : null;
        boolean isPublished = resultSet.getBoolean("is_published");

        posts.add(new PostMetaData(title, slug, summary, published, lastEdit, isPublished));
      }

      return posts;

    } catch (SQLException e) {
      throw new RepoException("Error fetching all post metadata sorted by time", e);
    }
  }

  @Override
  public Optional<Post> getPost(String slug) {
    String sql = """
        SELECT title, slug, summary, content, published, last_edited, is_published
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
          String summary = resultSet.getString("summary");
          String content = resultSet.getString("content");
          Instant published = Instant.parse(resultSet.getString("published"));
          Instant lastEdited = Instant.parse(resultSet.getString("last_edited"));
          boolean isPublished = resultSet.getBoolean("is_published");

          return Optional.of(new Post(title, postSlug, summary, published, lastEdited, content, isPublished));
        }
        return Optional.empty();
      }

    } catch (SQLException e) {
      throw new RepoException("Error fetching full post by slug: " + slug, e);
    }
  }

  @Override
  public List<PostMetaData> searchPostsBody(String query) {
    String sql = """
        SELECT title, slug, summary, published, last_edited, is_published
        FROM posts
        WHERE content LIKE ?
        ORDER BY published DESC
        """;

    List<PostMetaData> posts = new ArrayList<>();

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      String likeParam = "%" + query + "%";
      stmt.setString(1, likeParam);

      try (ResultSet resultSet = stmt.executeQuery()) {
        while (resultSet.next()) {
          String title = resultSet.getString("title");
          String slug = resultSet.getString("slug");
          String summary = resultSet.getString("summary");
          Instant published = Instant.parse(resultSet.getString("published"));
          Instant lastEdit = Instant.parse(resultSet.getString("last_edited"));
          boolean isPublished = resultSet.getBoolean("is_published");

          posts.add(new PostMetaData(title, slug, summary, published, lastEdit, isPublished));
        }
      }

      return posts;

    } catch (SQLException e) {
      throw new RepoException("Error searching posts for query: " + query, e);
    }
  }

  @Override
  public void addPost(Post post) {
    String sql = """
        INSERT INTO posts (slug, title, content, summary, published, last_edited, is_published)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, post.slug());
      stmt.setString(2, post.title());
      stmt.setString(3, post.content());
      stmt.setString(4, post.summary());
      stmt.setString(5, post.timePublished().toString());
      stmt.setString(6, post.timePublished().toString());
      stmt.setBoolean(7, post.isPublished());

      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RepoException("Error adding posts post: " + post.slug(), e);
    }
  }

  @Override
  public void updatePost(Post post) {
    String sql = """
        UPDATE posts
        SET title = ?, content = ?, summary = ?, published = ?, last_edited = ?, is_published = ?
        WHERE slug = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, post.title());
      stmt.setString(2, post.content());
      stmt.setString(3, post.summary());
      stmt.setString(4, post.timePublished().toString());
      stmt.setString(5, Instant.now().toString());
      stmt.setBoolean(6, post.isPublished());
      stmt.setString(7, post.slug());

      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RepoException("Error updating posts post: " + post.slug(), e);
    }
  }

  @Override
  public void deletePost(String slug) {
    String sql = """
        DELETE FROM posts
        WHERE slug = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, slug);

      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RepoException("Error deleting posts post: " + slug, e);
    }
  }

  @Override
  public boolean existsPostBySlug(String slug) {
    String sql = """
        SELECT 1
        FROM posts
        WHERE slug = ?
        LIMIT 1
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, slug);

      try (ResultSet resultSet = stmt.executeQuery()) {
        return resultSet.next();
      }

    } catch (SQLException e) {
      throw new RepoException("Error checking existence of post by slug: " + slug, e);
    }
  }
}
