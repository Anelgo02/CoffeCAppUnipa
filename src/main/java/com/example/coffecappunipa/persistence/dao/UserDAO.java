package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.model.User;
import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.sql.*;
import java.util.Optional;
import java.math.BigDecimal;

public class UserDAO {

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, email, role, credit FROM users WHERE username = ?";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new DaoException("Errore findByUsername()", e);
        }
    }

    public long createCustomer(String username, String email) {
        String sql = "INSERT INTO users(username, email, role, credit) VALUES(?, ?, 'CUSTOMER', 0.00)";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.setString(2, email);

            int updated = ps.executeUpdate();
            if (updated != 1) throw new DaoException("Inserimento user fallito (righe modificate=" + updated + ")");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new DaoException("ID generato non trovato.");
                return keys.getLong(1);
            }

        } catch (SQLException e) {
            throw new DaoException("Errore createCustomer()", e);
        }
    }

    public BigDecimal topUpCredit(long userId, BigDecimal amount) {
        String updateSql = "UPDATE users SET credit = credit + ? WHERE id = ?";
        String selectSql = "SELECT credit FROM users WHERE id = ?";

        try (Connection conn = DbConnectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psUp = conn.prepareStatement(updateSql);
                 PreparedStatement psSel = conn.prepareStatement(selectSql)) {

                psUp.setBigDecimal(1, amount);
                psUp.setLong(2, userId);

                int updated = psUp.executeUpdate();
                if (updated != 1) throw new DaoException("TopUp fallito (righe modificate=" + updated + ")");

                psSel.setLong(1, userId);
                try (ResultSet rs = psSel.executeQuery()) {
                    if (!rs.next()) throw new DaoException("Credito non letto dopo topUp.");
                    BigDecimal newCredit = rs.getBigDecimal("credit");
                    conn.commit();
                    return newCredit;
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DaoException("Errore topUpCredit()", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setRole(rs.getString("role"));
        u.setCredit(rs.getBigDecimal("credit"));
        return u;
    }
}
