package no.eliashaugsbakk.clams.server.utils;

import io.javalin.http.Context;
import java.util.Map;

// BEGIN LLM EDIT: Centralize content-aware error responses for browser and API clients.
/**
 * Shared HTTP error responses.
 *
 * <p>Disclaimer: This response helper was written by an LLM to keep API errors consistently
 * structured while preserving the site's HTML error page for browser-facing routes.</p>
 */
// END LLM EDIT
public final class ErrorResponses {
  private ErrorResponses() {
  }

  public static void badRequest(Context ctx, String message) {
    render(ctx, 400, message);
  }

  public static void unauthorized(Context ctx, String message) {
    render(ctx, 401, message);
  }

  public static void forbidden(Context ctx, String message) {
    render(ctx, 403, message);
  }

  public static void notFound(Context ctx, String message) {
    render(ctx, 404, message);
  }

  public static void conflict(Context ctx, String message) {
    render(ctx, 409, message);
  }

  public static void unsupportedMediaType(Context ctx, String message) {
    render(ctx, 415, message);
  }

  public static void serverError(Context ctx, String message) {
    render(ctx, 500, message);
  }

  private static void render(Context ctx, int status, String message) {
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
      case 415 -> "Unsupported Media Type";
      default -> "Internal Server Error";
    };
  }
}
