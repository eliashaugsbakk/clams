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
import no.eliashaugsbakk.clams.server.model.Article;
import no.eliashaugsbakk.clams.server.model.ArticleMetaData;
import no.eliashaugsbakk.clams.server.repository.ArticlesRepo;
import no.eliashaugsbakk.clams.server.repository.ArticlesRepoSqlite;
import no.eliashaugsbakk.clams.server.repository.SqliteManager;
import no.eliashaugsbakk.clams.server.service.ArticlesSearchService;
import no.eliashaugsbakk.clams.server.utils.MarkdownConverter;

public class ArticlesController {
  private static final ZoneId OSLO_ZONE = ZoneId.of("Europe/Oslo");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

  private final ArticlesRepo articlesRepo;
  private final ArticlesSearchService articlesSearchService;

  public record ArticleItem(String title, String slug, String summary, String formattedDate, int year) {}

  public ArticlesController(SqliteManager sqliteManager) {
    this.articlesRepo = new ArticlesRepoSqlite(sqliteManager);
    this.articlesSearchService = new ArticlesSearchService(articlesRepo);
  }

  public void handleGetArticle(Context ctx) {
    var articleOpt = articlesRepo.getArticle(ctx.pathParam("slug"));

    if (articleOpt.isEmpty()) {
      ctx.status(404);
      return;
    }

    Article article = articleOpt.get();

    if (!article.isPublished()) {
      ctx.status(404);
      return;
    }

    String formattedDate = article.timePublished().atZone(OSLO_ZONE).format(DATE_FORMATTER);

    ctx.render("templates/article.html", Map.of(
        "page_title", article.title() + " -- Elias Haugsbakk",
        "page_css", "article",
        "published_date", formattedDate,
        "author", "by Elias Haugsbakk",
        "content", MarkdownConverter.convertToHtml(article.content()),
        "back_to_index", "<a href=\"/articles/\">back to all articles</a>"
    ));
  }

  public void handleGetArticles(Context ctx) {
    String searchTerm = ctx.queryParam("search");
    if (searchTerm != null) {
      handleSearch(ctx, searchTerm);
    } else {
      handleArticlesIndex(ctx);
    }
  }

  private void handleArticlesIndex(Context ctx) {
    List<ArticleMetaData> allArticles = articlesRepo.listArticlesMetaData().stream()
        .filter(ArticleMetaData::isPublished)
        .toList();

    Map<Integer, List<ArticleMetaData>> articlesByYear = groupArticlesByYear(allArticles);

    Map<Integer, List<ArticleItem>> articleItemsByYear = articlesByYear.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream()
                .map(article -> new ArticleItem(
                    article.title(),
                    article.slug(),
                    article.summary() != null ? article.summary() : "",
                    article.timePublished().atZone(OSLO_ZONE).format(DATE_FORMATTER),
                    article.timePublished().atZone(OSLO_ZONE).getYear()
                ))
                .toList()
        ));

    ctx.render("templates/articles.html",
        Map.of(
            "page_title", "My articles",
            "page_css", "articles",
            "articles_by_year", articleItemsByYear,
            "search_value", ""
        ));
  }

  private void handleSearch(Context ctx, String searchTerm) {
    String query = searchTerm.trim();
    List<ArticleMetaData> results = query.isEmpty() ? List.of() : articlesSearchService.searchArticles(query);

    List<ArticleItem> resultItems = results.stream()
        .filter(ArticleMetaData::isPublished)
        .map(article -> new ArticleItem(
            article.title(),
            article.slug(),
            article.summary() != null ? article.summary() : "",
            article.timePublished().atZone(OSLO_ZONE).format(DATE_FORMATTER),
            article.timePublished().atZone(OSLO_ZONE).getYear()
        ))
        .toList();

    ctx.render("templates/articles-search.html",
        Map.of(
            "page_title", "Search Results -- Elias Haugsbakk",
            "page_css", "articles",
            "search_term", query,
            "results", resultItems,
            "results_count", resultItems.size()
        ));
  }

  private Map<Integer, List<ArticleMetaData>> groupArticlesByYear(List<ArticleMetaData> articles) {
    Map<Integer, List<ArticleMetaData>> unsortedGroups = articles.stream()
        .collect(Collectors.groupingBy(
            article -> article.timePublished().atZone(OSLO_ZONE).getYear(),
            Collectors.toList()
        ));

    Map<Integer, List<ArticleMetaData>> sortedGroups = new TreeMap<>(Comparator.reverseOrder());
    sortedGroups.putAll(unsortedGroups);

    return sortedGroups;
  }
}
