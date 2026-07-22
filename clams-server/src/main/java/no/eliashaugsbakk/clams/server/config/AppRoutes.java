package no.eliashaugsbakk.clams.server.config;

import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import io.javalin.apibuilder.EndpointGroup;
import java.util.Map;

public class AppRoutes implements EndpointGroup {
  private final AppContext appContext;

  public AppRoutes(AppContext appContext) {
    this.appContext = appContext;
  }

  @Override
  public void addEndpoints() {
    get("/", ctx -> ctx.redirect("/home"));
    get("/home", ctx -> ctx.render("templates/home.html",
        Map.of("page_title", "Elias Haugsbakk", "page_css", "home")));

    path("posts", () -> {
      get(appContext.getPostsController()::handleGetPosts);
      get("{slug}", appContext.getPostsController()::handleGetPost);
    });

    path("projects", () -> get(appContext.getProjectsController()::handleGetProjects));

    path("api", () -> {
      before("*", ctx -> {
        String authHeader = ctx.header("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
          ctx.status(401).json(Map.of("error", "Unauthorized",
              "message", "Missing or malformed Authorization header."));
          ctx.skipRemainingHandlers();
          return;
        }

        String token = authHeader.substring(7).trim();
        if (!appContext.getAuthService().isValid(token)) {
          ctx.status(403)
              .json(Map.of("error", "Forbidden",
                  "message", "Invalid API validation token."));
          ctx.skipRemainingHandlers();
        }
      });

      post("posts", appContext.getPostController()::handlePostPost);
      put("posts/{slug}", appContext.getPostController()::handlePutPost);
      delete("posts/{slug}", appContext.getPostController()::handleDeletePost);

      get("projects", appContext.getProjectsController()::handleGetProjectsApi);
      post("projects", appContext.getProjectsController()::handlePostProject);
      put("projects/{id}", appContext.getProjectsController()::handlePutProject);
      delete("projects/{id}", appContext.getProjectsController()::handleDeleteProject);

      get("media", appContext.getMediaController()::handleGetMediaIndex);
      get("media/{uuid}", appContext.getMediaController()::handleGetMedia);
      post("media", appContext.getMediaController()::handlePostMedia);
      delete("media/{uuid}", appContext.getMediaController()::handleDeleteMedia);
    });
  }
}
