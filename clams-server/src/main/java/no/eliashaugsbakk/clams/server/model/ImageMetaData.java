package no.eliashaugsbakk.clams.server.model;

import java.time.Instant;
import java.util.UUID;

public record ImageMetaData(
    UUID uuid,
    String originalFilename,
    String contentType,
    Instant timeUploaded
) {}
