package no.eliashaugsbakk.clams.server.model;

import java.time.Instant;

// BEGIN LLM EDIT: Allow authenticated clients to provide an explicit publication timestamp.
/**
 * Blog post mutation payload.
 *
 * <p>Disclaimer: The optional publication timestamp field was written by an LLM to allow
 * clients to correct publication dates without a database migration.</p>
 */
// END LLM EDIT
public record PostDTO(String title, String summary, String content, boolean isPublished,
                      Instant publishedAt) {
}
