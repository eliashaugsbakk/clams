package no.eliashaugsbakk.clams.server.model;

import java.time.Instant;

public record Post(String title, String slug, Instant timePublished, String content) {
}
