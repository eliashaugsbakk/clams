package no.eliashaugsbakk.clams.server;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinPebble;
import java.util.Map;
import no.eliashaugsbakk.clams.server.config.AppContext;
import no.eliashaugsbakk.clams.server.config.AppRoutes;

public class Main {
  void main() {
    // Initialize dependencies
    AppContext context = new AppContext();

    try {
      Javalin.create(config -> {
        config.staticFiles.add("/public");
        config.fileRenderer(new JavalinPebble());

        config.routes.error(404, ctx -> {
          if (ctx.path().startsWith("/api")) {
            ctx.json(Map.of(
                "error", "Not Found",
                "message", "The requested API endpoint does not exist."
            ));
          } else {
            ctx.render("templates/404.html", Map.of(
                "page_title", "404 - Page Not Found",
                "page_css", "404"
            ));
          }
        });

        config.routes.exception(UnrecognizedPropertyException.class, (e, ctx) -> ctx.status(400).json(Map.of(
            "error", "Bad Request",
            "message", "Unrecognized property: '" + e.getPropertyName() + "'"
        )));

        config.routes.exception(Exception.class, (e, ctx) -> {
          e.printStackTrace();
          ctx.status(500);

          if (ctx.path().startsWith("/api")) {
            ctx.json(Map.of("error", "Internal Server Error"));
          } else {
            ctx.result("Internal Server Error. Please try again later.");
          }
        });

        config.routes.apiBuilder(new AppRoutes(context));

        config.events.serverStopped(context::close);
      }).start(7070);


    } catch (Exception e) {
      context.close();
      throw e;
    }
  }
}
