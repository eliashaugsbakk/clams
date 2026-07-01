package no.eliashaugsbakk.clams.server;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinPebble;
import no.eliashaugsbakk.clams.server.controller.BlogController;
import no.eliashaugsbakk.clams.server.repository.SqliteManager;

public class Main {
  void main() {
    // TODO: Change db storage location to .../share/clams/... and implement custom storage locations
    SqliteManager dbManager = new SqliteManager("testDb.db");
    dbManager.init();

    BlogController blogController = new BlogController();

    var app = Javalin.create(config -> {
      config.staticFiles.add("/public");
      config.fileRenderer(new JavalinPebble());

      config.routes.error(404, ctx -> ctx.render("templates/404.html"));

      config.routes.get("/", ctx -> ctx.redirect("/home"));
      config.routes.get("/home", ctx -> ctx.render("templates/home.html"));
      config.routes.get("/projects", ctx -> ctx.render("templates/projects.html"));

      config.routes.get("/blog/{slug}", blogController::handleGetPost);
      config.routes.get("/blog", blogController::handleGetOverview);

      config.events.serverStopped(dbManager::close);
    }).start(7070);
  }
}
