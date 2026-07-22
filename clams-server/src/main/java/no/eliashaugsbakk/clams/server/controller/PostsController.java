package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostMetaData;
import no.eliashaugsbakk.clams.server.repository.PostsRepo;
import no.eliashaugsbakk.clams.server.repository.PostsRepoSqlite;
import no.eliashaugsbakk.clams.server.repository.SqliteManager;
import no.eliashaugsbakk.clams.server.service.PostsSearchService;
import no.eliashaugsbakk.clams.server.utils.MarkdownConverter;

public class PostsController {
  private static final ZoneId OSLO_ZONE = ZoneId.of("Europe/Oslo");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

  private final PostsRepo postsRepo;
  private final PostsSearchService postsSearchService;

  public record PostItem(String title, String slug, String summary, String formattedDate, int year) {}

  public PostsController(SqliteManager sqliteManager) {
    this.postsRepo = new PostsRepoSqlite(sqliteManager);
    this.postsSearchService = new PostsSearchService(postsRepo);
  }

  public void handleGetPost(Context ctx) {
    var postOpt = postsRepo.getPost(ctx.pathParam("slug"));

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
        "published_date", formattedDate,
        "author", "by Elias Haugsbakk",
        "content", MarkdownConverter.convertToHtml(post.content()),
        "back_to_index", "<a href=\"/posts/\">back to all posts</a>"
    ));
  }

  public void handleGetPosts(Context ctx) {
    String searchTerm = ctx.queryParam("search");
    if (searchTerm != null) {
      handleSearch(ctx, searchTerm);
    } else {
      handlePostsIndex(ctx);
    }
  }

  private void handlePostsIndex(Context ctx) {
    List<PostMetaData> allPosts = postsRepo.listPostsMetaData().stream()
        .filter(PostMetaData::isPublished)
        .toList();

    Map<Integer, List<PostMetaData>> postsByYear = groupPostsByYear(allPosts);

    Map<Integer, List<PostItem>> postItemsByYear = postsByYear.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream()
                .map(post -> new PostItem(
                    post.title(),
                    post.slug(),
                    post.summary() != null ? post.summary() : "",
                    post.timePublished().atZone(OSLO_ZONE).format(DATE_FORMATTER),
                    post.timePublished().atZone(OSLO_ZONE).getYear()
                ))
                .toList()
        ));

    ctx.render("templates/posts.html",
        Map.of(
            "page_title", "My posts",
            "page_css", "posts",
            "posts_by_year", postItemsByYear,
            "search_value", ""
        ));
  }

  private void handleSearch(Context ctx, String searchTerm) {
    String query = searchTerm.trim();
    List<PostMetaData> results = query.isEmpty() ? List.of() : postsSearchService.searchPosts(query);

    List<PostItem> resultItems = results.stream()
        .filter(PostMetaData::isPublished)
        .map(post -> new PostItem(
            post.title(),
            post.slug(),
            post.summary() != null ? post.summary() : "",
            post.timePublished().atZone(OSLO_ZONE).format(DATE_FORMATTER),
            post.timePublished().atZone(OSLO_ZONE).getYear()
        ))
        .toList();

    ctx.render("templates/posts-search.html",
        Map.of(
            "page_title", "Search Results -- Elias Haugsbakk",
            "page_css", "posts",
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
