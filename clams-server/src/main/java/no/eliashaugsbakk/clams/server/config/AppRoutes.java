package no.eliashaugsbakk.clams.server.config;

import static io.javalin.apibuilder.ApiBuilder.*;
import io.javalin.apibuilder.EndpointGroup;
import java.util.Map;

public class AppRoutes implements EndpointGroup {
  private final AppContext ctx;

  public AppRoutes(AppContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void addEndpoints() {
    get("/", ctx -> ctx.redirect("/home"));
    get("/home", ctx -> ctx.render("templates/home.html",
        Map.of("page_title", "Elias Haugsbakk", "page_css", "home")));
    get("/projects", ctx -> ctx.render("templates/projects.html",
        Map.of("page_title", "Projects - Elias Haugsbakk", "page_css", "projects")));

    path("blog", () -> {
      get(ctx.getBlogController()::handleBlogRequest);
      get("{slug}", ctx.getBlogController()::handleGetPost);
    });

    path("api", () -> {
      post("posts", ctx.getPostController()::handlePostPost);
      put("posts/{slug}", ctx.getPostController()::handlePutPost);
      delete("posts/{slug}", ctx.getPostController()::handleDeletePost);

      get("media", ctx.getMediaController()::handleGetMediaIndex);
      get("media/{uuid}", ctx.getMediaController()::handleGetMedia);
      post("media", ctx.getMediaController()::handlePostMedia);
      delete("media/{uuid}", ctx.getMediaController()::handleDeleteMedia);
    });
  }
}
