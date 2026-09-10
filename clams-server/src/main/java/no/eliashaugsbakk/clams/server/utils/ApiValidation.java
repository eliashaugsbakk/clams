package no.eliashaugsbakk.clams.server.utils;

import io.javalin.http.BadRequestResponse;

// BEGIN LLM EDIT: Added shared request validation for stable API input constraints.
/**
 * Validation helpers for authenticated API request payloads.
 *
 * <p>Disclaimer: This validation utility was written by an LLM to keep request constraints
 * consistent across the API controllers.</p>
 */
// END LLM EDIT
public final class ApiValidation {
  private ApiValidation() {
  }

  public static String requiredText(String field, String value, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new BadRequestResponse("Field '" + field + "' is required.");
    }
    return bounded(field, value, maxLength);
  }

  public static String optionalText(String field, String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return value;
    }
    return bounded(field, value, maxLength);
  }

  private static String bounded(String field, String value, int maxLength) {
    if (value.length() > maxLength) {
      throw new BadRequestResponse(
          "Field '" + field + "' must be at most " + maxLength + " characters.");
    }
    return value;
  }
}
