package no.eliashaugsbakk.clams.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
        SELECT id, title, slug, summary, created_at, published_at, updated_at, is_published
        FROM posts
        ORDER BY published_at DESC NULLS LAST
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
        Instant created = parseInstant(resultSet.getString("created_at"), id, "created_at");
        String publishedRaw = resultSet.getString("published_at");
        Instant published = publishedRaw == null ? null : parseInstant(publishedRaw, id, "published_at");
        Instant updated = parseInstant(resultSet.getString("updated_at"), id, "updated_at");
        boolean isPublished = resultSet.getBoolean("is_published");

        posts.add(new PostMetaData(id, title, slug, summary, created, published, updated, isPublished));
      }

      return posts;

    } catch (SQLException e) {
      throw new RepoException("Error fetching all post metadata sorted by time", e);
    }
  }

  @Override
  public Optional<Post> getPost(long id) {
    String sql = """
        SELECT id, title, slug, summary, content, created_at, published_at, updated_at, is_published
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
          Instant created = parseInstant(resultSet.getString("created_at"), id, "created_at");
          String publishedRaw = resultSet.getString("published_at");
          Instant published = publishedRaw == null ? null : parseInstant(publishedRaw, id, "published_at");
          Instant updated = parseInstant(resultSet.getString("updated_at"), id, "updated_at");
          boolean isPublished = resultSet.getBoolean("is_published");

          return Optional.of(new Post(postId, title, postSlug, summary, created, published, updated, content,
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
        SELECT id, title, slug, summary, created_at, published_at, updated_at, is_published
        FROM posts
        WHERE content LIKE ?
        ORDER BY published_at DESC NULLS LAST
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
          Instant created = parseInstant(resultSet.getString("created_at"), id, "created_at");
          String publishedRaw = resultSet.getString("published_at");
          Instant published = publishedRaw == null ? null : parseInstant(publishedRaw, id, "published_at");
          Instant updated = parseInstant(resultSet.getString("updated_at"), id, "updated_at");
          boolean isPublished = resultSet.getBoolean("is_published");

          posts.add(new PostMetaData(id, title, slug, summary, created, published, updated, isPublished));
        }

      }

      return posts;

    } catch (SQLException e) {
      throw new RepoException("Error searching posts for query: " + query, e);
    }
  }

  private Instant parseInstant(String value, long postId, String column) {
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException | NullPointerException e) {
      throw new RepoException("Invalid timestamp in posts." + column + " for post " + postId, e);
    }
  }

  @Override
  public long addPost(Post post) {
    // BEGIN LLM EDIT: Bind all eight post columns when inserting a new post.
    String sql = """
        INSERT INTO posts (slug, title, content, summary, created_at, published_at, updated_at, is_published)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
    // END LLM EDIT

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      stmt.setString(1, post.slug());
      stmt.setString(2, post.title());
      stmt.setString(3, post.content());
      stmt.setString(4, post.summary());
      stmt.setString(5, post.createdAt().toString());
      stmt.setString(6, post.publishedAt() == null ? null : post.publishedAt().toString());
      stmt.setString(7, post.updatedAt().toString());
      stmt.setBoolean(8, post.isPublished());

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
        SET title = ?, content = ?, summary = ?, published_at = ?, updated_at = ?, is_published = ?
        WHERE id = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, post.title());
      stmt.setString(2, post.content());
      stmt.setString(3, post.summary());
      stmt.setString(4, post.publishedAt() == null ? null : post.publishedAt().toString());
      stmt.setString(5, post.updatedAt().toString());
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
