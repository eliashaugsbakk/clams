package no.eliashaugsbakk.webserver.db;

public interface TokenRepository {
    boolean addToken(String token);
    boolean removeToken(String token);
    boolean isValid(String token);
}
