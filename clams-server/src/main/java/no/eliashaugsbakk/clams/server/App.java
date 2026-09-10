package no.eliashaugsbakk.clams.server;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.rendering.template.JavalinPebble;
import no.eliashaugsbakk.clams.server.config.AppContext;
import no.eliashaugsbakk.clams.server.config.AppRoutes;
import no.eliashaugsbakk.clams.server.utils.ErrorResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
  private static final Logger log = LoggerFactory.getLogger(App.class);

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
        config.routes.error(400, ctx -> ErrorResponses.badRequest(ctx, "The request could not be understood."));
        config.routes.error(401, ctx -> ErrorResponses.unauthorized(ctx, "Authentication is required."));
        config.routes.error(403, ctx -> ErrorResponses.forbidden(ctx, "You are not allowed to access this resource."));
        config.routes.error(404, ctx -> ErrorResponses.notFound(ctx, "The requested resource was not found."));
        config.routes.error(409, ctx -> ErrorResponses.conflict(ctx, "The request conflicts with existing data."));
        config.routes.error(415, ctx -> ErrorResponses.unsupportedMediaType(ctx,
            "The request media type is not supported."));
        config.routes.error(500, ctx -> ErrorResponses.serverError(ctx, "An unexpected server error occurred."));
        // END LLM EDIT

        config.routes.exception(UnrecognizedPropertyException.class,
            (e, ctx) -> ErrorResponses.badRequest(ctx,
                "Unrecognized property: '" + e.getPropertyName() + "'"));

        // BEGIN LLM EDIT: Normalize validation and conflict exceptions through the shared responder.
        config.routes.exception(BadRequestResponse.class,
            (e, ctx) -> ErrorResponses.badRequest(ctx, e.getMessage()));
        config.routes.exception(ConflictResponse.class,
            (e, ctx) -> ErrorResponses.conflict(ctx, e.getMessage()));
        config.routes.exception(NotFoundResponse.class,
            (e, ctx) -> ErrorResponses.notFound(ctx, e.getMessage()));
        // END LLM EDIT

        config.routes.exception(Exception.class, (e, ctx) -> {
          log.error("Unhandled error on {} {}", ctx.method(), ctx.path(), e);
          ErrorResponses.serverError(ctx, "An unexpected server error occurred.");
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
