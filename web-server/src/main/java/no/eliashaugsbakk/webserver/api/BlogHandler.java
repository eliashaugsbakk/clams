package no.eliashaugsbakk.webserver.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import no.eliashaugsbakk.webserver.db.PageRepository;
import no.eliashaugsbakk.webserver.model.Page;
import no.eliashaugsbakk.webserver.model.PageMetadata;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BlogHandler implements HttpHandler {
    private static final String AUTHOR = "Elias Haugsbakk";
    private static final int RECENT_POST_LIMIT = 5;
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Oslo");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE.withZone(DISPLAY_ZONE);
    private static final DateTimeFormatter POST_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy").withZone(DISPLAY_ZONE);

    private final PageRepository pageRepo;

    public BlogHandler(PageRepository pageRepo) {
        this.pageRepo = pageRepo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path.equals("/blog") || path.equals("/blog/")) {
                sendResponse(exchange, 200, renderBlogIndex(exchange));
                return;
            }

            Optional<String> slug = getSlug(path);
            if (slug.isEmpty()) {
                sendResponse(exchange, 404, "Not found");
                return;
            }

            Optional<Page> page = pageRepo.findPageBySlug(slug.get()).filter(Page::published);
            if (page.isEmpty()) {
                sendResponse(exchange, 404, "Not found");
                return;
            }

            sendResponse(exchange, 200, renderPost(page.get()));
        } catch (Exception e) {
            System.err.println("Error handling blog request: " + e.getMessage());
            sendResponse(exchange, 500, "Internal server error");
        } finally {
            exchange.close();
        }
    }

    private String renderBlogIndex(HttpExchange exchange) throws IOException {
        String search = getQueryParameter(exchange, "search");
        List<PageMetadata> posts = search.isBlank()
                ? pageRepo.getAllPageMetadata()
                : pageRepo.searchMetadataInTitle(search);
        posts = posts.stream().filter(PageMetadata::published).toList();

        String template = readTemplate("blog.html");
        return template
                .replace("${search_value}", escapeHtml(search))
                .replace("${all_posts_title}", search.isBlank() ? "All" : "Search results")
                .replace("${all_posts_by_year}", renderPostsByYear(posts))
                .replace("${recent_posts}", renderRecentPosts(posts));
    }

    private String renderPost(Page page) throws IOException {
        String template = readTemplate("post.html");
        return template
                .replace("${title}", escapeHtml(page.title()))
                .replace("${published_date}", escapeHtml(formatPostDate(page)))
                .replace("${author}", AUTHOR)
                .replace("${content}", page.html() == null ? "" : page.html())
                .replace("${previous_post_link}", renderAdjacentLink(pageRepo.findPreviousPageMetadata(page.slug()), "prev-post", "&larr; "))
                .replace("${next_post_link}", renderAdjacentLink(pageRepo.findNextPageMetadata(page.slug()), "next-post", "", " &rarr;"));
    }

    private String renderPostsByYear(List<PageMetadata> posts) {
        if (posts.isEmpty()) {
            return "<p>No posts found.</p>";
        }

        Map<String, List<PageMetadata>> postsByYear = new LinkedHashMap<>();
        for (PageMetadata post : posts) {
            String year = getDisplayInstant(post)
                    .map(instant -> String.valueOf(instant.atZone(DISPLAY_ZONE).getYear()))
                    .orElse("Undated");
            postsByYear.computeIfAbsent(year, ignored -> new ArrayList<>()).add(post);
        }

        StringBuilder html = new StringBuilder();
        for (Map.Entry<String, List<PageMetadata>> entry : postsByYear.entrySet()) {
            html.append("<details open>")
                    .append("<summary>")
                    .append(escapeHtml(entry.getKey()))
                    .append(" (")
                    .append(entry.getValue().size())
                    .append(")</summary><ul>");

            for (PageMetadata post : entry.getValue()) {
                html.append("<li><a href=\"/blog/")
                        .append(urlEncodePathSegment(post.slug()))
                        .append("\">")
                        .append(escapeHtml(post.title()))
                        .append("</a></li>");
            }

            html.append("</ul></details>");
        }
        return html.toString();
    }

    private String renderRecentPosts(List<PageMetadata> posts) {
        if (posts.isEmpty()) {
            return "<p>No posts found.</p>";
        }

        StringBuilder html = new StringBuilder("<ul>");
        posts.stream().limit(RECENT_POST_LIMIT).forEach(post ->
                html.append("<li>")
                        .append(escapeHtml(formatMetadataDate(post)))
                        .append(" -- <a href=\"/blog/")
                        .append(urlEncodePathSegment(post.slug()))
                        .append("\">")
                        .append(escapeHtml(post.title()))
                        .append("</a></li>"));
        html.append("</ul>");
        return html.toString();
    }

    private String renderAdjacentLink(Optional<PageMetadata> maybePost, String cssClass, String prefix) {
        return renderAdjacentLink(maybePost, cssClass, prefix, "");
    }

    private String renderAdjacentLink(Optional<PageMetadata> maybePost, String cssClass, String prefix, String suffix) {
        return maybePost
                .filter(PageMetadata::published)
                .map(post -> "<a href=\"/blog/" + urlEncodePathSegment(post.slug()) + "\" class=\"" + cssClass + "\">"
                        + prefix + escapeHtml(post.title()) + suffix + "</a>")
                .orElse("<span></span>");
    }

    private Optional<String> getSlug(String path) {
        String prefix = "/blog/";
        if (!path.startsWith(prefix)) {
            return Optional.empty();
        }

        String slug = path.substring(prefix.length());
        if (slug.endsWith("/")) {
            slug = slug.substring(0, slug.length() - 1);
        }

        if (slug.isBlank() || slug.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(URLDecoder.decode(slug, StandardCharsets.UTF_8));
    }

    private String getQueryParameter(HttpExchange exchange, String name) {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }

        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) {
                try {
                    return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    return "";
                }
            }
        }
        return "";
    }

    private String formatPostDate(Page page) {
        return getDisplayInstant(page.metadata())
                .map(POST_DATE_FORMATTER::format)
                .orElse("");
    }

    private String formatMetadataDate(PageMetadata post) {
        return getDisplayInstant(post)
                .map(ISO_DATE_FORMATTER::format)
                .orElse("Undated");
    }

    private Optional<Instant> getDisplayInstant(PageMetadata post) {
        if (post.publishedAt() != null) {
            return Optional.of(post.publishedAt());
        }
        if (post.lastEditedAt() != null) {
            return Optional.of(post.lastEditedAt());
        }
        return Optional.empty();
    }

    private String readTemplate(String fileName) throws IOException {
        Path appTemplate = Path.of("/app/templates", fileName);
        if (Files.exists(appTemplate)) {
            return Files.readString(appTemplate);
        }

        try (var stream = getClass().getResourceAsStream("/templates/" + fileName)) {
            if (stream == null) {
                throw new IOException("Template not found: " + fileName);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String urlEncodePathSegment(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
