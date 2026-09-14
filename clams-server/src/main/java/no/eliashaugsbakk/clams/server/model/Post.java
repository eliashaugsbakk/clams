package no.eliashaugsbakk.clams.server.model;

import java.time.Instant;

// BEGIN LLM EDIT: Make the existing database ID the immutable post identity.
/**
 * Blog post model.
 *
 * <p>Disclaimer: The numeric identity and explicit timestamp changes in this model were written
 * by an LLM.</p>
 */
// END LLM EDIT
public record Post(Long id, String title, String slug, String summary, Instant createdAt,
                   Instant publishedAt, Instant updatedAt, String content, boolean isPublished) {
  public Post(PostDTO postDTO, String slug) {
    this(null, postDTO.title(), slug, postDTO.summary(), Instant.now(),
        postDTO.isPublished()
            ? (postDTO.publishedAt() == null ? Instant.now() : postDTO.publishedAt())
            : null,
        Instant.now(), postDTO.content(), postDTO.isPublished());
  }

  public static Post fromUpdated(Post existing, PostDTO postDTO) {
    return new Post(existing.id(), postDTO.title(), existing.slug(), postDTO.summary(),
        existing.createdAt(), postDTO.isPublished()
            ? (postDTO.publishedAt() == null
                ? (existing.publishedAt() == null ? Instant.now() : existing.publishedAt())
                : postDTO.publishedAt())
            : null,
        Instant.now(),
        postDTO.content(), postDTO.isPublished());
  }
}
