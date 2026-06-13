package no.eliashaugsbakk.webserver.db;

/**
 * Custom runtime exception to decouple the database implementation from the rest of the application.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
