package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import no.eliashaugsbakk.clams.server.model.PostMetaData;
import no.eliashaugsbakk.clams.server.repository.PostsRepo;

// BEGIN LLM EDIT: Added a public summary-only RSS 2.0 feed for published blog posts.
/**
 * Generates the public blog RSS feed.
 *
 * <p>Disclaimer: This controller was written by an LLM. It publishes metadata and summaries,
 * not full post content; review the public URL and XML escaping before deployment.</p>
 */
public class RssController {
  private static final DateTimeFormatter RSS_DATE_FORMAT =
      DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);

  private final PostsRepo postsRepo;
  private final String configuredSiteUrl;

  public RssController(PostsRepo postsRepo, String configuredSiteUrl) {
    this.postsRepo = postsRepo;
    this.configuredSiteUrl = configuredSiteUrl;
  }

  public void handleGetFeed(Context ctx) {
    String siteUrl = configuredSiteUrl.isBlank() ? requestOrigin(ctx) : configuredSiteUrl;
    String feedUrl = siteUrl + "/rss.xml";
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>%s</title>
            <link>%s</link>
            <description>Latest blog posts</description>
            <language>en</language>
            %s
          </channel>
        </rss>
        """.formatted(
        escapeXml("Elias Haugsbakk"),
        escapeXml(siteUrl),
        postsRepo.listPostsMetaData().stream()
            .filter(post -> Boolean.TRUE.equals(post.isPublished()))
            .filter(post -> post.slug() != null && post.title() != null && post.timePublished() != null)
            .sorted((left, right) -> right.timePublished().compareTo(left.timePublished()))
            .map(post -> itemXml(post, siteUrl))
            .collect(Collectors.joining("\n")));

    ctx.contentType("application/rss+xml; charset=UTF-8");
    ctx.header("Cache-Control", "public, max-age=900");
    ctx.result(xml);
  }

  private static String itemXml(PostMetaData post, String siteUrl) {
    String postUrl = siteUrl + "/posts/" + post.slug();
    String summary = post.summary() == null ? "" : post.summary();
    return """
        <item>
          <title>%s</title>
          <link>%s</link>
          <guid isPermaLink="true">%s</guid>
          <pubDate>%s</pubDate>
          <description>%s</description>
        </item>
        """.formatted(
        escapeXml(post.title()),
        escapeXml(postUrl),
        escapeXml(postUrl),
        RSS_DATE_FORMAT.format(post.timePublished()),
        escapeXml(summary));
  }

  private static String requestOrigin(Context ctx) {
    URI requestUri = URI.create(ctx.url());
    return requestUri.getScheme() + "://" + requestUri.getRawAuthority();
  }

  private static String escapeXml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}
// END LLM EDIT
