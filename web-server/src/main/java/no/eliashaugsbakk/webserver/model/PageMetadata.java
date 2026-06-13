package no.eliashaugsbakk.webserver.model;

import java.time.Instant;

public record PageMetadata(
        String slug,
        String title,
        String summary,
        Instant publishedAt,
        Instant lastEditedAt,
        boolean published
) {}
