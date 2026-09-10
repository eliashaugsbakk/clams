package no.eliashaugsbakk.clams.server.model;

import java.time.Instant;

// BEGIN LLM EDIT: Make the existing database ID the immutable post identity.
/**
 * Blog post model.
 *
 * <p>Disclaimer: The numeric identity changes in this model were written by an LLM.</p>
 */
// END LLM EDIT
public record Post(Long id, String title, String slug, String summary, Instant timePublished,
                   Instant lastEdited, String content, boolean isPublished) {
  public Post(PostDTO postDTO, String slug) {
    this(null, postDTO.title(), slug, postDTO.summary(), Instant.now(), Instant.now(), postDTO.content(),
        postDTO.isPublished());
  }

  public static Post fromUpdated(Post existing, PostDTO postDTO) {
    return new Post(existing.id(), postDTO.title(), existing.slug(), postDTO.summary(),
        existing.timePublished(), Instant.now(),
        postDTO.content(), postDTO.isPublished());
  }
}
