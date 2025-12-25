package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConnectionDAO {

    public void connect(long customerId, long distributorId) {
        String closePrev = "UPDATE customer_connections SET disconnected_at = NOW() " +
                "WHERE customer_id = ? AND disconnected_at IS NULL";

        String insertNew = "INSERT INTO customer_connections(customer_id, distributor_id) VALUES(?, ?)";

        try (Connection conn = DbConnectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(closePrev);
                 PreparedStatement ps2 = conn.prepareStatement(insertNew)) {

                ps1.setLong(1, customerId);
                ps1.executeUpdate();

                ps2.setLong(1, customerId);
                ps2.setLong(2, distributorId);
                int ins = ps2.executeUpdate();
                if (ins != 1) throw new DaoException("Inserimento connessione fallito (righe=" + ins + ")");

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DaoException("Errore ConnectionDAO.connect()", e);
        }
    }

    public void disconnect(long customerId) {
        String sql = "UPDATE customer_connections SET disconnected_at = NOW() " +
                "WHERE customer_id = ? AND disconnected_at IS NULL";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DaoException("Errore ConnectionDAO.disconnect()", e);
        }
    }

    public Long findActiveDistributorId(long customerId) {
        String sql = "SELECT distributor_id FROM customer_connections " +
                "WHERE customer_id = ? AND disconnected_at IS NULL " +
                "ORDER BY connected_at DESC LIMIT 1";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getLong("distributor_id");
            }

        } catch (SQLException e) {
            throw new DaoException("Errore ConnectionDAO.findActiveDistributorId()", e);
        }
    }

    public String findActiveDistributorCodeByCustomerId(long customerId) {
        String sql =
                "SELECT d.code " +
                        "FROM customer_connections c " +
                        "JOIN distributors d ON d.id = c.distributor_id " +
                        "WHERE c.customer_id = ? AND c.disconnected_at IS NULL " +
                        "ORDER BY c.connected_at DESC " +
                        "LIMIT 1";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("code");
            }

        } catch (SQLException e) {
            throw new DaoException("Errore findActiveDistributorCodeByCustomerId()", e);
        }
    }

}
