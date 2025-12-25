package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BeverageDAO {

    public static class BeverageRow {
        public long id;
        public String name;
        public BigDecimal price;

        public BeverageRow(long id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }

    public List<BeverageRow> findActive() {
        String sql = "SELECT id, name, price FROM beverages WHERE is_active = 1 ORDER BY name";
        List<BeverageRow> out = new ArrayList<>();

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new BeverageRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price")
                ));
            }
            return out;

        } catch (SQLException e) {
            throw new DaoException("Errore BeverageDAO.findActive()", e);
        }
    }

    public Optional<BeverageRow> findById(long beverageId) {
        String sql = "SELECT id, name, price FROM beverages WHERE id = ? AND is_active = 1";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, beverageId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new BeverageRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price")
                ));
            }

        } catch (SQLException e) {
            throw new DaoException("Errore BeverageDAO.findById()", e);
        }
    }
}
