package no.eliashaugsbakk.webserver;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinPebble;
import no.eliashaugsbakk.webserver.controller.BlogController;

public class Main {

  void main() {
    BlogController blogController = new BlogController();

    var app = Javalin.create(config -> {
      config.staticFiles.add("/public");
      config.fileRenderer(new JavalinPebble());

      config.routes.error(404, ctx -> ctx.render("templates/404.html"));

      config.routes.get("/", ctx -> ctx.redirect("/home"));
      config.routes.get("/home", ctx -> ctx.render("templates/home.html"));
      config.routes.get("/portfolio", ctx -> ctx.render("templates/portfolio.html"));

      config.routes.get("/blog/{slug}", blogController::handleGetPost);
      config.routes.get("/blog", blogController::handleGetBlogOverview);

    }).start(8080);
  }
}
