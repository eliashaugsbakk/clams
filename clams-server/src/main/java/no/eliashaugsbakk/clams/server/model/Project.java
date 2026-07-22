package no.eliashaugsbakk.clams.server.model;

public record Project(long id, String name, String readMoreUrl, String gitUrl, String gitHubUrl, String description) {
}
