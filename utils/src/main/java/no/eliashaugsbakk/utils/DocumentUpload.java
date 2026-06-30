package no.eliashaugsbakk.utils;

import java.util.List;

/**
 * Contains the data of a new post to upload to the server.
 */
public record DocumentUpload(
    String title,
    String rawContent,
    String summary,
    List<String> tags,
    List<Image> images
) {}
