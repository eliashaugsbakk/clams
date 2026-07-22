package no.eliashaugsbakk.clams.server.model;

import java.time.Instant;

public record Post(String title, String slug, String summary, Instant timePublished, Instant lastEdited, String content,
                   boolean isPublished) {
  public Post(PostDTO postDTO, String slug) {

    this(postDTO.title(), slug, postDTO.summary(), Instant.now(), Instant.now(), postDTO.content(),
        postDTO.isPublished());
  }
}
