package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.sql.*;

public class DistributorAdminDAO {

    public long createDistributorWithSupplies(String code, String locationName, String statusEnum) {
        String insD = "INSERT INTO distributors(code, location_name, status) VALUES(?, ?, ?)";
        String insS = "INSERT INTO distributor_supplies(distributor_id, coffee_level, milk_level, sugar_level, cups_level) VALUES(?, 0, 0, 0, 0)";

        try (Connection conn = DbConnectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psD = conn.prepareStatement(insD, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psS = conn.prepareStatement(insS)) {

                psD.setString(1, code);
                psD.setString(2, locationName);
                psD.setString(3, statusEnum);

                int dUpd = psD.executeUpdate();
                if (dUpd != 1) throw new DaoException("Inserimento distributors fallito (updated=" + dUpd + ")");

                long distId;
                try (ResultSet keys = psD.getGeneratedKeys()) {
                    if (!keys.next()) throw new DaoException("ID generato non trovato (distributors).");
                    distId = keys.getLong(1);
                }

                psS.setLong(1, distId);
                int sUpd = psS.executeUpdate();
                if (sUpd != 1) throw new DaoException("Inserimento distributor_supplies fallito (updated=" + sUpd + ")");

                conn.commit();
                return distId;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorAdminDAO.createDistributorWithSupplies()", e);
        }
    }

    public void deleteDistributorByCode(String code) {
        String sql = "DELETE FROM distributors WHERE code = ?";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, code);

            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new DaoException("Delete distributor fallita: non trovato (code=" + code + ")");
            }

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorAdminDAO.deleteDistributorByCode()", e);
        }
    }

    public void updateStatusByCode(String code, String statusEnum) {
        String sql = "UPDATE distributors SET status = ? WHERE code = ?";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, statusEnum);
            ps.setString(2, code);

            int updated = ps.executeUpdate();
            if (updated != 1) throw new DaoException("Update status fallito: non trovato (code=" + code + ")");

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorAdminDAO.updateStatusByCode()", e);
        }
    }

    public static String uiStatusToDbEnum(String ui) {
        if (ui == null) return null;
        String s = ui.trim().toUpperCase();
        // UI: ATTIVO / IN MANUTENZIONE / DISATTIVO
        if (s.equals("ATTIVO")) return "ACTIVE";
        if (s.equals("MANUTENZIONE") || s.equals("IN MANUTENZIONE")) return "MAINTENANCE";
        if (s.equals("DISATTIVO")  || "FAULT".equals(s) || "GUASTO".equals(s)) return "FAULT"; // nel tuo enum non c’è DISABLED, uso FAULT come “non operativo”
        return null;
    }
}
