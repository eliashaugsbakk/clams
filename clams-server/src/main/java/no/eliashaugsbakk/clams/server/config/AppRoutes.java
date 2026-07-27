package no.eliashaugsbakk.clams.server.config;

import static io.javalin.apibuilder.ApiBuilder.after;
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
    before(ctx -> {
      String path = ctx.path();

      // Skip non-GET requests, API endpoints, static resources, and queried requests
      if (!ctx.method().name().equalsIgnoreCase("GET")
          || path.startsWith("/api")
          || isStaticResource(path)
          || ctx.queryString() != null) {
        return;
      }

      String cachedHtml = appContext.pageCache().get(path);
      if (cachedHtml != null) {
        ctx.result(cachedHtml);
        ctx.contentType("text/html");
        ctx.skipRemainingHandlers();
      }
    });

    after(ctx -> {
      String method = ctx.method().name();
      String path = ctx.path();

      // Invalidate cache on any modifying request if successful
      if ((method.equalsIgnoreCase("POST")
          || method.equalsIgnoreCase("PUT")
          || method.equalsIgnoreCase("DELETE"))
          && ctx.status().getCode() >= 200 && ctx.status().getCode() < 300) {
        appContext.pageCache().clear();
        return;
      }

      // 2. Cache GET responses for HTML pages
      String contentType = ctx.contentType();
      if (method.equalsIgnoreCase("GET")
          && !path.startsWith("/api") // exclude API responses
          && !isStaticResource(path) // exclude static assets
          && ctx.queryString() == null // exclude queried pages
          && ctx.status().getCode() == 200
          && contentType != null
          && contentType.contains("text/html")) { // only cache HTML responses

        String renderedHtml = ctx.result();
        if (renderedHtml != null && !renderedHtml.isBlank()) {
          appContext.pageCache().put(path, renderedHtml);
        }
      }
    });


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
          ctx.status(401).json(Map.of("error", "Unauthorized", "message",
              "Missing or malformed Authorization header."));
          ctx.skipRemainingHandlers();
          return;
        }

        String token = authHeader.substring(7).trim();
        if (!appContext.getAuthService().isValid(token)) {
          ctx.status(403)
              .json(Map.of("error", "Forbidden", "message", "Invalid API validation token."));
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

  private static boolean isStaticResource(String path) {
    if (path == null) {
      return false;
    }
    return path.startsWith("/css/")
        || path.startsWith("/images/");
  }
}
