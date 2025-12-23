package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaintainerDAO {

    public static class MaintainerRow {
        public long userId;
        public String maintainerId; // username in users (es. M-001)
        public String firstName;
        public String lastName;
        public String email;
        public String phone;
    }

    public List<MaintainerRow> findAllWithProfile() {
        String sql =
                "SELECT u.id AS user_id, u.username, u.email, p.first_name, p.last_name, p.phone " +
                        "FROM users u " +
                        "JOIN maintainer_profiles p ON p.user_id = u.id " +
                        "WHERE u.role = 'MAINTAINER' " +
                        "ORDER BY u.username";

        List<MaintainerRow> out = new ArrayList<>();

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                MaintainerRow m = new MaintainerRow();
                m.userId = rs.getLong("user_id");
                m.maintainerId = rs.getString("username");
                m.email = rs.getString("email");
                m.firstName = rs.getString("first_name");
                m.lastName = rs.getString("last_name");
                m.phone = rs.getString("phone");
                out.add(m);
            }
            return out;

        } catch (SQLException e) {
            throw new DaoException("Errore MaintainerDAO.findAllWithProfile()", e);
        }
    }

    public Optional<Long> findUserIdByUsernameMaintainer(String username) {
        String sql = "SELECT id FROM users WHERE username = ? AND role = 'MAINTAINER'";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(rs.getLong("id"));
            }

        } catch (SQLException e) {
            throw new DaoException("Errore MaintainerDAO.findUserIdByUsernameMaintainer()", e);
        }
    }

    public long createMaintainer(String maintainerId, String firstName, String lastName, String email, String phone) {
        String insUser = "INSERT INTO users(username, email, role, credit) VALUES(?, ?, 'MAINTAINER', 0.00)";
        String insProf = "INSERT INTO maintainer_profiles(user_id, first_name, last_name, phone) VALUES(?, ?, ?, ?)";

        try (Connection conn = DbConnectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psU = conn.prepareStatement(insUser, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psP = conn.prepareStatement(insProf)) {

                psU.setString(1, maintainerId);
                psU.setString(2, email);

                int uUpd = psU.executeUpdate();
                if (uUpd != 1) throw new DaoException("Inserimento users fallito (updated=" + uUpd + ")");

                long userId;
                try (ResultSet keys = psU.getGeneratedKeys()) {
                    if (!keys.next()) throw new DaoException("ID generato non trovato (users).");
                    userId = keys.getLong(1);
                }

                psP.setLong(1, userId);
                psP.setString(2, firstName);
                psP.setString(3, lastName);
                psP.setString(4, phone);

                int pUpd = psP.executeUpdate();
                if (pUpd != 1) throw new DaoException("Inserimento maintainer_profiles fallito (updated=" + pUpd + ")");

                conn.commit();
                return userId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DaoException("Errore MaintainerDAO.createMaintainer()", e);
        }
    }

    public void deleteMaintainerById(String maintainerId) {
        // CASCADE -> maintainer_profiles verrà eliminato
        String sql = "DELETE FROM users WHERE username = ? AND role = 'MAINTAINER'";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, maintainerId);

            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new DaoException("Delete maintainer fallita: non trovato (id=" + maintainerId + ")");
            }

        } catch (SQLException e) {
            throw new DaoException("Errore MaintainerDAO.deleteMaintainerById()", e);
        }
    }
}
