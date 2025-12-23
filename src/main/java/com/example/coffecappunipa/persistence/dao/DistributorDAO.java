package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.model.DistributorState;
import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class DistributorDAO {

    public Long findIdByCode(String code) {
        String sql = "SELECT id FROM distributors WHERE code = ?";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getLong("id");
            }

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorDAO.findIdByCode()", e);
        }
    }

    public List<DistributorState> findAllStatesForXml() {
        String sql = "SELECT d.id, d.code, d.location_name, d.status, " +
                "COALESCE(s.coffee_level, 0) AS coffee_level, " +
                "COALESCE(s.milk_level, 0) AS milk_level, " +
                "COALESCE(s.sugar_level, 0) AS sugar_level, " +
                "COALESCE(s.cups_level, 0) AS cups_level " +
                "FROM distributors d " +
                "LEFT JOIN distributor_supplies s ON s.distributor_id = d.id " +
                "ORDER BY d.code";

        List<DistributorState> out = new ArrayList<>();
        Map<Long, DistributorState> byId = new HashMap<>();

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");

                DistributorState d = new DistributorState();
                d.setCode(rs.getString("code"));
                d.setLocationName(rs.getString("location_name"));
                d.setStatus(rs.getString("status"));

                d.setCoffeeLevel(rs.getInt("coffee_level"));
                d.setMilkLevel(rs.getInt("milk_level"));
                d.setSugarLevel(rs.getInt("sugar_level"));
                d.setCupsLevel(rs.getInt("cups_level"));

                out.add(d);
                byId.put(id, d);
            }

            loadOpenFaults(conn, byId);
            return out;

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorDAO.findAllStatesForXml()", e);
        }
    }

    private void loadOpenFaults(Connection conn, Map<Long, DistributorState> byId) throws SQLException {
        String sql = "SELECT id, distributor_id, description, created_at " +
                "FROM distributor_faults " +
                "WHERE is_open = 1 " +
                "ORDER BY distributor_id, created_at DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long faultId = rs.getLong("id");
                long distId = rs.getLong("distributor_id");

                DistributorState dist = byId.get(distId);
                if (dist == null) continue;

                DistributorState.FaultItem f = new DistributorState.FaultItem();
                f.setCode("F-" + faultId);
                f.setDescription(rs.getString("description"));

                Timestamp ts = rs.getTimestamp("created_at");
                LocalDateTime dt = (ts != null) ? ts.toLocalDateTime() : null;
                f.setCreatedAt(dt);

                dist.getFaults().add(f);
            }
        }
    }

    public void refillSuppliesByCode(String code, int coffee, int milk, int sugar, int cups) {
        String sql = "UPDATE distributor_supplies s " +
                "JOIN distributors d ON d.id = s.distributor_id " +
                "SET s.coffee_level = ?, s.milk_level = ?, s.sugar_level = ?, s.cups_level = ? " +
                "WHERE d.code = ?";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, coffee);
            ps.setInt(2, milk);
            ps.setInt(3, sugar);
            ps.setInt(4, cups);
            ps.setString(5, code);

            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new DaoException("Refill fallito: distributore/supplies non trovato (code=" + code + ")");
            }

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorDAO.refillSuppliesByCode()", e);
        }
    }

    public void updateStatusByCode(String code, String dbStatus) {
        String sql = "UPDATE distributors SET status = ? WHERE code = ?";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dbStatus);
            ps.setString(2, code);

            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new DaoException("Update status fallito: distributore non trovato (code=" + code + ")");
            }

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorDAO.updateStatusByCode()", e);
        }
    }
}
