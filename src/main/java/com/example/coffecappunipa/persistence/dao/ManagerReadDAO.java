package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ManagerReadDAO {

    public List<String[]> lastPurchases(int limit) {
        // columns output: [id, username, distributor_code, beverage_name, amount, created_at]

        String sql =
                "SELECT p.id, u.username, d.code, b.name, p.price_paid, p.created_at " +
                        "FROM purchases p " +
                        "JOIN users u ON u.id = p.customer_id " +
                        "JOIN distributors d ON d.id = p.distributor_id " +
                        "JOIN beverages b ON b.id = p.beverage_id " +
                        "ORDER BY p.created_at DESC " +
                        "LIMIT ?";

        List<String[]> out = new ArrayList<>();

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new String[] {
                            rs.getString(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6)
                    });
                }
            }

            return out;

        } catch (SQLException e) {
            throw new DaoException("Errore ManagerReadDAO.lastPurchases()", e);
        }
    }

    public List<String[]> lastTopups(int limit) {
        // columns output: [id, username, amount, created_at]
        String sql =
                "SELECT t.id, u.username, t.amount, t.created_at " +
                        "FROM topups t " +
                        "JOIN users u ON u.id = t.customer_id " +
                        "ORDER BY t.created_at DESC " +
                        "LIMIT ?";

        List<String[]> out = new ArrayList<>();

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new String[] {
                            rs.getString(1),
                            rs.getString(2),
                            rs.getString(3),
                            rs.getString(4)
                    });
                }
            }

            return out;

        } catch (SQLException e) {
            throw new DaoException("Errore ManagerReadDAO.lastTopups()", e);
        }
    }

    public List<String[]> distributorsList() {
        // columns output: [code, location_name, status]
        String sql = "SELECT code, location_name, status FROM distributors ORDER BY code";
        List<String[]> out = new ArrayList<>();

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new String[] {
                        rs.getString("code"),
                        rs.getString("location_name"),
                        rs.getString("status")
                });
            }
            return out;

        } catch (SQLException e) {
            throw new DaoException("Errore ManagerReadDAO.distributorsList()", e);
        }
    }
}
