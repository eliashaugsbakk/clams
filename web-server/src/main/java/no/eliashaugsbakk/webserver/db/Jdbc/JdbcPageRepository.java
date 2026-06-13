package no.eliashaugsbakk.webserver.db.Jdbc;

import no.eliashaugsbakk.webserver.db.DataAccessException;
import no.eliashaugsbakk.webserver.db.PageRepository;
import no.eliashaugsbakk.webserver.model.Page;
import no.eliashaugsbakk.webserver.model.PageMetadata;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class JdbcPageRepository implements PageRepository {
    private final DatabaseManager dbManager;

    public JdbcPageRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Optional<Page> findPageBySlug(String slug) {
        String sql = """
                SELECT slug, title, summary, published_at, last_edited_at,
                       is_published, content_raw, content_html
                FROM posts
                WHERE slug = ?
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slug);
            try (ResultSet resultSet = stmt.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapFullPage(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching page by slug: " + slug, e);
        }
    }

    @Override
    public List<PageMetadata> getAllPageMetadata() {
        String sql = """
                SELECT slug, title, summary, published_at, last_edited_at, is_published
                FROM posts
                ORDER BY COALESCE(published_at, last_edited_at, '') DESC, id DESC
                """;
        List<PageMetadata> result = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                result.add(mapPageMetadata(resultSet));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching page metadata", e);
        }
        return result;
    }

    @Override
    public Optional<PageMetadata> findPreviousPageMetadata(String slug) {
        String sql = """
                WITH current_page AS (
                    SELECT COALESCE(published_at, last_edited_at, '') AS sort_time, id
                    FROM posts
                    WHERE slug = ?
                )
                SELECT p.slug, p.title, p.summary, p.published_at, p.last_edited_at, p.is_published
                FROM posts p, current_page c
                WHERE COALESCE(p.published_at, p.last_edited_at, '') > c.sort_time
                   OR (COALESCE(p.published_at, p.last_edited_at, '') = c.sort_time AND p.id > c.id)
                ORDER BY COALESCE(p.published_at, p.last_edited_at, '') ASC, p.id ASC
                LIMIT 1
                """;

        return findAdjacentMetadata(slug, sql, "previous");
    }

    @Override
    public Optional<PageMetadata> findNextPageMetadata(String slug) {
        String sql = """
                WITH current_page AS (
                    SELECT COALESCE(published_at, last_edited_at, '') AS sort_time, id
                    FROM posts
                    WHERE slug = ?
                )
                SELECT p.slug, p.title, p.summary, p.published_at, p.last_edited_at, p.is_published
                FROM posts p, current_page c
                WHERE COALESCE(p.published_at, p.last_edited_at, '') < c.sort_time
                   OR (COALESCE(p.published_at, p.last_edited_at, '') = c.sort_time AND p.id < c.id)
                ORDER BY COALESCE(p.published_at, p.last_edited_at, '') DESC, p.id DESC
                LIMIT 1
                """;

        return findAdjacentMetadata(slug, sql, "next");
    }

    @Override
    public boolean addPage(Page page) {
        String sql =
                """
                INSERT INTO posts (
                    slug, title, content_raw, content_html, summary,
                    published_at, last_edited_at, is_published
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, page.slug());
            stmt.setString(2, page.title());
            stmt.setString(3, page.rawContent());
            stmt.setString(4, page.html());
            stmt.setString(5, page.summary());
            stmt.setString(6, toDbInstant(page.publishedAt()));
            stmt.setString(7, toDbInstant(page.lastEditedAt()));
            stmt.setBoolean(8, page.published());

            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Error adding page: " + page.title(), e);
        }
    }

    @Override
    public List<PageMetadata> searchMetadataInTitle(String query) {

        String sanitized = query
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

        String sql =
                """
                SELECT slug, title, summary, published_at, last_edited_at, is_published
                FROM posts
                WHERE title LIKE ? ESCAPE '!'
                ORDER BY COALESCE(published_at, last_edited_at, '') DESC, id DESC
                """;
        List<PageMetadata> result = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + sanitized + "%");

            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapPageMetadata(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error searching pages with query: " + query, e);
        }
        return result;
    }

    @Override
    public Set<String> getAllSlugs() {
        String sql =
                """
                SELECT slug FROM posts
                """;
        Set<String> result = new HashSet<>();

        try (Connection conn = dbManager.getConnection();
             Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                result.add(resultSet.getString("slug"));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching all slugs", e);
        }
        return result;
    }

    @Override
    public boolean slugExists(String slug) {
        String sql = "SELECT 1 FROM posts WHERE slug = ? LIMIT 1";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slug);

            try (ResultSet resultSet = stmt.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error checking slug: " + slug, e);
        }
    }

    private Optional<PageMetadata> findAdjacentMetadata(String slug, String sql, String direction) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slug);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapPageMetadata(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error fetching " + direction + " page metadata for slug: " + slug, e);
        }
    }

    private PageMetadata mapPageMetadata(ResultSet resultSet) throws SQLException {
        return new PageMetadata(
                resultSet.getString("slug"),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                fromDbInstant(resultSet.getString("published_at")),
                fromDbInstant(resultSet.getString("last_edited_at")),
                resultSet.getBoolean("is_published")
        );
    }

    private Page mapFullPage(ResultSet resultSet) throws SQLException {
        return new Page(
                resultSet.getString("slug"),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                fromDbInstant(resultSet.getString("published_at")),
                fromDbInstant(resultSet.getString("last_edited_at")),
                resultSet.getBoolean("is_published"),
                resultSet.getString("content_raw"),
                resultSet.getString("content_html")
        );
    }

    private String toDbInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private Instant fromDbInstant(String value) {
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }
}
