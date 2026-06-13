package no.eliashaugsbakk.webserver.db.Jdbc;

import no.eliashaugsbakk.webserver.db.DataAccessException;
import no.eliashaugsbakk.webserver.db.TokenRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcTokenRepository implements TokenRepository {

    private final DatabaseManager dbManager;

    public JdbcTokenRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public boolean addToken(String token) {
        String sql =
                """
                INSERT INTO tokens (tokenValue) VALUES (?)
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Error adding token", e);
        }
    }


    @Override
    public boolean removeToken(String token) {
        String sql =
                """
                DELETE FROM tokens WHERE tokenValue = ?;
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Error removing token", e);
        }
    }

    @Override
    public boolean isValid(String token) {
        String sql =
                """
                SELECT 1 FROM tokens WHERE tokenValue = ?;
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error validating token", e);
        }
    }
}
