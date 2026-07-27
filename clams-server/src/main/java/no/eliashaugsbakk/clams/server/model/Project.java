package no.eliashaugsbakk.clams.server.model;

public record Project(Long id, String name, String readMoreUrl, String gitUrl, String gitHubUrl, String description) {
}
