package no.eliashaugsbakk.webserver.model;

import java.time.Instant;

public record Page(
        String slug,
        String title,
        String summary,
        Instant publishedAt,
        Instant lastEditedAt,
        boolean published,
        String rawContent,
        String html
) {
    public Page(String slug, String title, Instant publishedAt, String html) {
        this(slug, title, null, publishedAt, publishedAt, true, null, html);
    }

    public PageMetadata metadata() {
        return new PageMetadata(slug, title, summary, publishedAt, lastEditedAt, published);
    }
}
