package no.eliashaugsbakk.clams.server.model;

import java.time.Instant;

public record PostMetaData(String title, String slug, Instant published, Instant lastEdit) {
}
