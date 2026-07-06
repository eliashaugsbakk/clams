package no.eliashaugsbakk.clams.server.model;

import java.time.Instant;

public record PostMetaData(String title, String slug, String summary, Instant timePublished, Instant lastEdit) {
}
