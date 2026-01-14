package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.model.DistributorState;
import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class DistributorDAO {

    public static class SyncResult {
        public int updated;
        public int missing;
        public int invalid;

        public SyncResult(int updated, int missing, int invalid) {
            this.updated = updated;
            this.missing = missing;
            this.invalid = invalid;
        }
    }

    public String findStatusByCode(String code) {
        String sql = "SELECT status FROM distributors WHERE code = ?";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("status");
            }

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorDAO.findStatusByCode()", e);
        }
    }

    // --- METODI PER SICUREZZA TOKEN (SPRING SECURITY) ---

    /**
     * Cerca il codice del distributore dato il token di sicurezza.
     * Usato dal Filtro di Spring Security per autenticare le richieste.
     */
    public String findCodeBySecurityToken(String token) {
        String sql = "SELECT code FROM distributors WHERE security_token = ?";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("code"); // Es: "UNIPA-001"
                }
                return null; // Token non valido o non trovato
            }
        } catch (SQLException e) {
            throw new DaoException("Errore findCodeBySecurityToken", e);
        }
    }

    /**
     * Aggiorna (o inserisce) il token di sicurezza per un distributore.
     * Chiamato dalla DistributorBootServlet al momento dell'avvio.
     */
    public void updateSecurityToken(String code, String token) {
        String sql = "UPDATE distributors SET security_token = ? WHERE code = ?";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (token == null) ps.setNull(1, java.sql.Types.VARCHAR);
            else ps.setString(1, token);
            ps.setString(2, code);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                // Se non ha aggiornato nulla, forse il codice non esiste
                throw new DaoException("Impossibile aggiornare token: codice non trovato " + code);
            }

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorDAO.updateSecurityToken()", e);
        }
    }

    // ----------------------------------------------------

    public String findSecurityTokenByCode(String code) throws DaoException {
        String sql = "SELECT security_token FROM distributors WHERE code = ?";
        try (Connection con = DbConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("security_token"); // può essere null
                return null;
            }
        } catch (SQLException e) {
            throw new DaoException("Errore findSecurityTokenByCode()", e);
        }
    }


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

    public SyncResult applyStatusesFromMonitor(Map<String, String> monitorStatuses) {
        if (monitorStatuses == null || monitorStatuses.isEmpty()) {
            return new SyncResult(0, 0, 0);
        }

        String sql = "UPDATE distributors SET status = ? WHERE code = ?";

        Connection conn = null;
        int updated = 0;
        int missing = 0;
        int invalid = 0;

        try {
            conn = DbConnectionManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<String, String> e : monitorStatuses.entrySet()) {
                    String code = (e.getKey() == null) ? "" : e.getKey().trim();
                    String stRaw = (e.getValue() == null) ? "" : e.getValue().trim();

                    if (code.isEmpty()) {
                        invalid++;
                        continue;
                    }

                    String dbStatus = normalizeMonitorStatus(stRaw);
                    if (dbStatus == null) {
                        invalid++;
                        continue;
                    }

                    ps.setString(1, dbStatus);
                    ps.setString(2, code);

                    int r = ps.executeUpdate();
                    if (r == 1) updated++;
                    else missing++;
                }
            }

            conn.commit();
            return new SyncResult(updated, missing, invalid);

        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            if (ex instanceof DaoException) throw (DaoException) ex;
            throw new DaoException("Errore DistributorDAO.applyStatusesFromMonitor()", ex);

        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private String normalizeMonitorStatus(String status) {
        if (status == null) return null;
        String s = status.trim().toUpperCase();

        if ("ACTIVE".equals(s)) return "ACTIVE";
        if ("MAINTENANCE".equals(s)) return "MAINTENANCE";
        if ("FAULT".equals(s)) return "FAULT";

        if ("ATTIVO".equals(s)) return "ACTIVE";
        if ("MANUTENZIONE".equals(s)) return "MAINTENANCE";
        if ("GUASTO".equals(s)) return "FAULT";

        return null;
    }
}