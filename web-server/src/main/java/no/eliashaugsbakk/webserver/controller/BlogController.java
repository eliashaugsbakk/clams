package no.eliashaugsbakk.webserver.controller;

import io.javalin.http.Context;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class BlogController {
  public void handleGetPost(Context ctx) {
    String slug = ctx.pathParam("slug");

    ctx.render("templates/post.html", Map.of(
        "title", "place holder",
        "published_date", "place holder",
        "author", "place holder",
        "content", "place holder"
    ));
  }

  public void handleGetBlogOverview(Context ctx) {
    ctx.render("templates/blog.html", Map.of(
        "all_posts_by_year", "place holder",
        "recent_posts", " place holder"
    ));
  }
}
