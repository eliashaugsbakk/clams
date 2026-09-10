package no.eliashaugsbakk.clams.server;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.rendering.template.JavalinPebble;
import java.util.Map;
import no.eliashaugsbakk.clams.server.config.AppContext;
import no.eliashaugsbakk.clams.server.config.AppRoutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
  private static final Logger log = LoggerFactory.getLogger(App.class);

  // BEGIN LLM EDIT: Keep browser errors rendered as HTML while API errors remain JSON.
  private static void renderError(io.javalin.http.Context ctx, int status, String message) {
    ctx.status(status);
    if (ctx.path().startsWith("/api")) {
      ctx.json(Map.of(
          "error", errorName(status),
          "message", message,
          "status", status
      ));
      return;
    }

    ctx.render("templates/404.html", Map.of(
        "page_title", status + " - Error",
        "page_css", "404",
        "error_code", status,
        "error_title", errorName(status),
        "error_message", message
    ));
  }

  private static String errorName(int status) {
    return switch (status) {
      case 400 -> "Bad Request";
      case 401 -> "Unauthorized";
      case 403 -> "Forbidden";
      case 404 -> "Not Found";
      case 409 -> "Conflict";
      default -> "Internal Server Error";
    };
  }
  // END LLM EDIT

  void main() {
    // Initialize dependencies
    AppContext context = new AppContext();

    try {
      Javalin.create(config -> {
        config.staticFiles.add(staticFiles -> {
          staticFiles.hostedPath = "/";
          staticFiles.directory = "/public";
        });
        config.fileRenderer(new JavalinPebble());

        // BEGIN LLM EDIT: Use one content-aware error response policy for all common statuses.
        config.routes.error(400, ctx -> renderError(ctx, 400, "The request could not be understood."));
        config.routes.error(401, ctx -> renderError(ctx, 401, "Authentication is required."));
        config.routes.error(403, ctx -> renderError(ctx, 403, "You are not allowed to access this resource."));
        config.routes.error(404, ctx -> renderError(ctx, 404, "The requested resource was not found."));
        config.routes.error(409, ctx -> renderError(ctx, 409, "The request conflicts with existing data."));
        config.routes.error(500, ctx -> renderError(ctx, 500, "An unexpected server error occurred."));
        // END LLM EDIT

        config.routes.exception(UnrecognizedPropertyException.class,
            (e, ctx) -> renderError(ctx, 400,
                "Unrecognized property: '" + e.getPropertyName() + "'"));

        // BEGIN LLM EDIT: Normalize validation and conflict exceptions through the shared responder.
        config.routes.exception(BadRequestResponse.class,
            (e, ctx) -> renderError(ctx, 400, e.getMessage()));
        config.routes.exception(ConflictResponse.class,
            (e, ctx) -> renderError(ctx, 409, e.getMessage()));
        config.routes.exception(NotFoundResponse.class,
            (e, ctx) -> renderError(ctx, 404, e.getMessage()));
        // END LLM EDIT

        config.routes.exception(Exception.class, (e, ctx) -> {
          log.error("Unhandled error on {} {}", ctx.method(), ctx.path(), e);
          renderError(ctx, 500, "An unexpected server error occurred.");
        });

        config.routes.apiBuilder(new AppRoutes(context));
        config.events.serverStopped(context::close);
      }).start(7070);


    } catch (Exception e) {
      context.close();
      log.error("Unexpected error", e);
      throw e;
    }
  }
}
