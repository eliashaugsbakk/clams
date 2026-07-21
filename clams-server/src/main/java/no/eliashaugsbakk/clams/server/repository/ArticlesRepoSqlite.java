package no.eliashaugsbakk.clams.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.eliashaugsbakk.clams.server.model.Article;
import no.eliashaugsbakk.clams.server.model.ArticleMetaData;

public class ArticlesRepoSqlite implements ArticlesRepo {
  private final SqliteManager manager;

  public ArticlesRepoSqlite(SqliteManager manager) {
    this.manager = manager;
  }

  @Override
  public List<ArticleMetaData> listArticlesMetaData() {
    String sql = """
        SELECT title, slug, summary, published, last_edited, is_published
        FROM articles
        ORDER BY published DESC
        """;

    List<ArticleMetaData> articles = new ArrayList<>();

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

        articles.add(new ArticleMetaData(title, slug, summary, published, lastEdit, isPublished));
      }

      return articles;

    } catch (SQLException e) {
      throw new RepoException("Error fetching all article metadata sorted by time", e);
    }
  }

  @Override
  public Optional<Article> getArticle(String slug) {
    String sql = """
        SELECT title, slug, summary, content, published, last_edited, is_published
        FROM articles
        WHERE slug = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, slug);

      try (ResultSet resultSet = stmt.executeQuery()) {
        if (resultSet.next()) {
          String title = resultSet.getString("title");
          String articleSlug = resultSet.getString("slug");
          String summary = resultSet.getString("summary");
          String content = resultSet.getString("content");
          Instant published = Instant.parse(resultSet.getString("published"));
          Instant lastEdited = Instant.parse(resultSet.getString("last_edited"));
          boolean isPublished = resultSet.getBoolean("is_published");

          return Optional.of(new Article(title, articleSlug, summary, published, lastEdited, content, isPublished));
        }
        return Optional.empty();
      }

    } catch (SQLException e) {
      throw new RepoException("Error fetching full article by slug: " + slug, e);
    }
  }

  @Override
  public List<ArticleMetaData> searchArticlesBody(String query) {
    String sql = """
        SELECT title, slug, summary, published, last_edited, is_published
        FROM articles
        WHERE content LIKE ?
        ORDER BY published DESC
        """;

    List<ArticleMetaData> articles = new ArrayList<>();

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

          articles.add(new ArticleMetaData(title, slug, summary, published, lastEdit, isPublished));
        }
      }

      return articles;

    } catch (SQLException e) {
      throw new RepoException("Error searching articles for query: " + query, e);
    }
  }

  @Override
  public void addArticle(Article article) {
    String sql = """
        INSERT INTO articles (slug, title, content, summary, published, last_edited, is_published)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, article.slug());
      stmt.setString(2, article.title());
      stmt.setString(3, article.content());
      stmt.setString(4, article.summary());
      stmt.setString(5, article.timePublished().toString());
      stmt.setString(6, article.timePublished().toString());
      stmt.setBoolean(7, article.isPublished());

      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RepoException("Error adding articles article: " + article.slug(), e);
    }
  }

  @Override
  public void updateArticle(Article article) {
    String sql = """
        UPDATE articles
        SET title = ?, content = ?, summary = ?, published = ?, last_edited = ?, is_published = ?
        WHERE slug = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, article.title());
      stmt.setString(2, article.content());
      stmt.setString(3, article.summary());
      stmt.setString(4, article.timePublished().toString());
      stmt.setString(5, Instant.now().toString());
      stmt.setBoolean(6, article.isPublished());
      stmt.setString(7, article.slug());

      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RepoException("Error updating articles article: " + article.slug(), e);
    }
  }

  @Override
  public void deleteArticle(String slug) {
    String sql = """
        DELETE FROM articles
        WHERE slug = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, slug);

      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RepoException("Error deleting articles article: " + slug, e);
    }
  }

  @Override
  public boolean existsArticleBySlug(String slug) {
    String sql = """
        SELECT 1
        FROM articles
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
      throw new RepoException("Error checking existence of article by slug: " + slug, e);
    }
  }
}
