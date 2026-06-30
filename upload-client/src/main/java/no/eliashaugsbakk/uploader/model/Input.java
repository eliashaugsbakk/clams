package no.eliashaugsbakk.uploader.model;

import java.util.List;

public record Input(
    List<String> filePaths,
    String url,
    String token,
    boolean generateToken,
    boolean networkTest,
    boolean helpRequested
) {}
