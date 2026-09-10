package no.eliashaugsbakk.clams.server.model;

import java.time.Instant;

public record PostMetaData(Long id, String title, String slug, String summary, Instant createdAt,
                           Instant publishedAt, Instant updatedAt, Boolean isPublished) {
}
