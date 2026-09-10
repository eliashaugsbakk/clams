package no.eliashaugsbakk.clams.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        SELECT id, title, slug, summary, published, last_edited, is_published
        FROM posts
        ORDER BY published DESC
        """;

    List<PostMetaData> posts = new ArrayList<>();

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet resultSet = stmt.executeQuery()) {

      while (resultSet.next()) {
        long id = resultSet.getLong("id");
        String title = resultSet.getString("title");
        String slug = resultSet.getString("slug");
        String summary = resultSet.getString("summary");
        Instant published = Instant.parse(resultSet.getString("published"));
        String lastEditRaw = resultSet.getString("last_edited");
        Instant lastEdit = (lastEditRaw != null) ? Instant.parse(lastEditRaw) : null;
        boolean isPublished = resultSet.getBoolean("is_published");

        posts.add(new PostMetaData(id, title, slug, summary, published, lastEdit, isPublished));
      }

      return posts;

    } catch (SQLException e) {
      throw new RepoException("Error fetching all post metadata sorted by time", e);
    }
  }

  @Override
  public Optional<Post> getPost(long id) {
    String sql = """
        SELECT id, title, slug, summary, content, published, last_edited, is_published
        FROM posts
        WHERE id = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setLong(1, id);

      try (ResultSet resultSet = stmt.executeQuery()) {
        if (resultSet.next()) {
          long postId = resultSet.getLong("id");
          String title = resultSet.getString("title");
          String postSlug = resultSet.getString("slug");
          String summary = resultSet.getString("summary");
          String content = resultSet.getString("content");
          Instant published = Instant.parse(resultSet.getString("published"));
          Instant lastEdited = Instant.parse(resultSet.getString("last_edited"));
          boolean isPublished = resultSet.getBoolean("is_published");

          return Optional.of(new Post(postId, title, postSlug, summary, published, lastEdited, content,
              isPublished));
        }
        return Optional.empty();
      }

    } catch (SQLException e) {
      throw new RepoException("Error fetching full post by ID: " + id, e);
    }
  }

  @Override
  public List<PostMetaData> searchPostsBody(String query) {
    String sql = """
        SELECT id, title, slug, summary, published, last_edited, is_published
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
          long id = resultSet.getLong("id");
          String title = resultSet.getString("title");
          String slug = resultSet.getString("slug");
          String summary = resultSet.getString("summary");
          Instant published = Instant.parse(resultSet.getString("published"));
          Instant lastEdit = Instant.parse(resultSet.getString("last_edited"));
          boolean isPublished = resultSet.getBoolean("is_published");

          posts.add(new PostMetaData(id, title, slug, summary, published, lastEdit, isPublished));
        }
      }

      return posts;

    } catch (SQLException e) {
      throw new RepoException("Error searching posts for query: " + query, e);
    }
  }

  @Override
  public long addPost(Post post) {
    String sql = """
        INSERT INTO posts (slug, title, content, summary, published, last_edited, is_published)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      stmt.setString(1, post.slug());
      stmt.setString(2, post.title());
      stmt.setString(3, post.content());
      stmt.setString(4, post.summary());
      stmt.setString(5, post.timePublished().toString());
      stmt.setString(6, post.timePublished().toString());
      stmt.setBoolean(7, post.isPublished());

      stmt.executeUpdate();
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          return generatedKeys.getLong(1);
        }
      }
      throw new RepoException("Post was inserted without a generated ID.");

    } catch (SQLException e) {
      throw new RepoException("Error adding posts post: " + post.slug(), e);
    }
  }

  @Override
  public void updatePost(Post post) {
    String sql = """
        UPDATE posts
        SET title = ?, content = ?, summary = ?, published = ?, last_edited = ?, is_published = ?
        WHERE id = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, post.title());
      stmt.setString(2, post.content());
      stmt.setString(3, post.summary());
      stmt.setString(4, post.timePublished().toString());
      stmt.setString(5, post.lastEdited().toString());
      stmt.setBoolean(6, post.isPublished());
      stmt.setLong(7, post.id());

      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RepoException("Error updating post: " + post.id(), e);
    }
  }

  @Override
  public boolean deletePost(long id) {
    String sql = """
        DELETE FROM posts
        WHERE id = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setLong(1, id);

      int rowsAffected = stmt.executeUpdate();
      return rowsAffected > 0;
    } catch (SQLException e) {
      throw new RepoException("Error deleting post " + id + ": ", e);
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

  // BEGIN LLM EDIT: Check stored post content before allowing an image deletion.
  @Override
  public List<String> findPostTitlesReferencing(String imageReference) {
    String sql = """
        SELECT title
        FROM posts
        WHERE content LIKE ?
        ORDER BY title ASC
        """;

    List<String> titles = new ArrayList<>();
    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, "%" + imageReference + "%");
      try (ResultSet resultSet = stmt.executeQuery()) {
        while (resultSet.next()) {
          titles.add(resultSet.getString("title"));
        }
      }
      return titles;
    } catch (SQLException e) {
      throw new RepoException("Error checking image references: " + imageReference, e);
    }
  }
  // END LLM EDIT
}
