package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import java.util.Map;

public class BlogController {
  public void handleGetPost(Context ctx) {
    ctx.render("templates/post.html",
        Map.of("title", "place holder Title",
            "published_date", "place holder Release Date",
            "author", "place holder Author",
            "content", "place holder Content"));
  }

  public void handleGetOverview(Context ctx) {
    ctx.render("templates/blog.html", Map.of(
    "all_posts_by_year", "place holder for all posts",
        "recent_posts", " place holder for recent posts"
    ));
  }
}
