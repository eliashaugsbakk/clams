package no.eliashaugsbakk.clams.server.model;

// BEGIN LLM EDIT: Add an additive project display-order field for editorial ordering.
/**
 * Project content model.
 *
 * <p>Disclaimer: The display-order field was added by an LLM. Existing API clients may omit it.</p>
 */
// END LLM EDIT
public record Project(Long id, String name, String readMoreUrl, String gitUrl, String gitHubUrl,
                      String description, Integer displayOrder) {
}
