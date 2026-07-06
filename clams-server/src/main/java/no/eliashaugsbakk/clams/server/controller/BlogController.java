package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostMetaData;
import no.eliashaugsbakk.clams.server.repository.BlogPostRepo;
import no.eliashaugsbakk.clams.server.repository.BlogPostRepoSqlite;
import no.eliashaugsbakk.clams.server.repository.SqliteManager;
import no.eliashaugsbakk.clams.server.service.BlogSearchService;
import no.eliashaugsbakk.clams.server.utils.MarkdownConverter;

public class BlogController {
  private static final ZoneId OSLO_ZONE = ZoneId.of("Europe/Oslo");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

  private final BlogPostRepo blogPostRepo;
  private final BlogSearchService blogSearchService;

  public record SearchResultItem(String title, String slug, String summary, String formattedDate) {}

  public BlogController(SqliteManager sqliteManager) {
    this.blogPostRepo = new BlogPostRepoSqlite(sqliteManager);
    this.blogSearchService = new BlogSearchService(blogPostRepo);
  }

  public void handleGetPost(Context ctx) {
    var postOpt = blogPostRepo.getPost(ctx.pathParam("slug"));

    if (postOpt.isEmpty()) {
      ctx.status(404);
      return;
    }

    Post post = postOpt.get();

    if (!post.isPublished()) {
      ctx.status(404);
      return;
    }

    String formattedDate = post.timePublished().atZone(OSLO_ZONE).format(DATE_FORMATTER);

    ctx.render("templates/post.html", Map.of(
        "page_title", post.title() + " -- Elias Haugsbakk",
        "page_css", "post",
        "title", post.title(),
        "published_date", formattedDate,
        "author", "by Elias Haugsbakk",
        "content", MarkdownConverter.convertToHtml(post.content()),
        "back_to_index", "<a href=\"/blog/\">back to all posts</a>"
    ));
  }

  public void handleBlogRequest(Context ctx) {
    String searchTerm = ctx.queryParam("search");
    if (searchTerm != null) {
      handleSearch(ctx, searchTerm);
    } else {
      handleBlogIndex(ctx);
    }
  }

  private void handleBlogIndex(Context ctx) {
    List<PostMetaData> allPosts = blogPostRepo.listPostsMetaData().stream()
        .filter(PostMetaData::isPublished)
        .toList();
    Map<Integer, List<PostMetaData>> postsByYear = groupPostsByYear(allPosts);

    // Manually list featured posts
    var featuredSlugs = List.of(
        ""
    );

    List<Post> featuredPosts = new ArrayList<>();
    for (String slug : featuredSlugs) {
      blogPostRepo.getPost(slug).ifPresent(featuredPosts::add);
    }

    ctx.render("templates/blog.html",
        Map.of(
            "page_title", "Elias Haugsbakk's Blog",
            "page_css", "blog",
            "posts_by_year", postsByYear,
            "featured_posts", featuredPosts,
            "search_value", ""
        ));
  }

  private void handleSearch(Context ctx, String searchTerm) {
    String query = searchTerm.trim();
    List<PostMetaData> results = query.isEmpty() ? List.of() : blogSearchService.searchPosts(query);

    List<SearchResultItem> resultItems = results.stream()
        .filter(PostMetaData::isPublished)
        .map(post -> new SearchResultItem(
            post.title(),
            post.slug(),
            post.summary() != null ? post.summary() : "",
            post.timePublished().atZone(OSLO_ZONE).format(DATE_FORMATTER)
        ))
        .toList();

    ctx.render("templates/blog-search.html",
        Map.of(
            "page_title", "Search Results -- Elias Haugsbakk",
            "page_css", "blog",
            "search_term", query,
            "results", resultItems,
            "results_count", resultItems.size()
        ));
  }

  private Map<Integer, List<PostMetaData>> groupPostsByYear(List<PostMetaData> posts) {
    Map<Integer, List<PostMetaData>> unsortedGroups = posts.stream()
        .collect(Collectors.groupingBy(
            post -> post.timePublished().atZone(OSLO_ZONE).getYear(),
            Collectors.toList()
        ));

    Map<Integer, List<PostMetaData>> sortedGroups = new TreeMap<>(Comparator.reverseOrder());
    sortedGroups.putAll(unsortedGroups);

    return sortedGroups;
  }
}
