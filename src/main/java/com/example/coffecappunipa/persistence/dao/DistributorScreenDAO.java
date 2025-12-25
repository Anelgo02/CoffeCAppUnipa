package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class DistributorScreenDAO {

    public static class ConnectedCustomer {
        public long customerId;
        public String username;
        public BigDecimal credit;

        public ConnectedCustomer(long customerId, String username, BigDecimal credit) {
            this.customerId = customerId;
            this.username = username;
            this.credit = credit;
        }
    }

    /**
     * Ritorna l'utente attualmente connesso al distributore (se esiste).
     * Connessione attiva = record customer_connections con disconnected_at IS NULL.
     */
    public Optional<ConnectedCustomer> findConnectedCustomerByDistributorCode(String distributorCode) {
        String sql =
                "SELECT u.id AS customer_id, u.username, u.credit " +
                        "FROM customer_connections cc " +
                        "JOIN distributors d ON d.id = cc.distributor_id " +
                        "JOIN users u ON u.id = cc.customer_id " +
                        "WHERE d.code = ? AND cc.disconnected_at IS NULL " +
                        "ORDER BY cc.connected_at DESC " +
                        "LIMIT 1";

        try (Connection conn = DbConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, distributorCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new ConnectedCustomer(
                        rs.getLong("customer_id"),
                        rs.getString("username"),
                        rs.getBigDecimal("credit")
                ));
            }

        } catch (SQLException e) {
            throw new DaoException("Errore DistributorScreenDAO.findConnectedCustomerByDistributorCode()", e);
        }
    }

    /**
     * Erogazione:
     * - verifica cliente connesso al distributore
     * - verifica credito >= prezzo
     * - inserisce purchase
     * - scala credito
     * Il tutto in transazione.
     */
    public BigDecimal performPurchase(
            String distributorCode,
            long beverageId,
            int sugarQty,
            BigDecimal beveragePrice
    ) {
        String lockConnSql =
                "SELECT u.id AS customer_id, u.credit, d.id AS distributor_id " +
                        "FROM customer_connections cc " +
                        "JOIN distributors d ON d.id = cc.distributor_id " +
                        "JOIN users u ON u.id = cc.customer_id " +
                        "WHERE d.code = ? AND cc.disconnected_at IS NULL " +
                        "ORDER BY cc.connected_at DESC " +
                        "LIMIT 1 " +
                        "FOR UPDATE";

        String insertPurchaseSql =
                "INSERT INTO purchases(customer_id, distributor_id, beverage_id, sugar_qty, price_paid) " +
                        "VALUES(?, ?, ?, ?, ?)";

        String updateCreditSql =
                "UPDATE users SET credit = credit - ? WHERE id = ?";

        String selectNewCreditSql =
                "SELECT credit FROM users WHERE id = ?";

        try (Connection conn = DbConnectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psLock = conn.prepareStatement(lockConnSql);
                 PreparedStatement psIns = conn.prepareStatement(insertPurchaseSql);
                 PreparedStatement psUp = conn.prepareStatement(updateCreditSql);
                 PreparedStatement psSel = conn.prepareStatement(selectNewCreditSql)) {

                // 1) lock + verifica connessione
                psLock.setString(1, distributorCode);
                long customerId;
                long distributorId;
                BigDecimal credit;

                try (ResultSet rs = psLock.executeQuery()) {
                    if (!rs.next()) {
                        throw new DaoException("Nessun cliente connesso al distributore.");
                    }
                    customerId = rs.getLong("customer_id");
                    distributorId = rs.getLong("distributor_id");
                    credit = rs.getBigDecimal("credit");
                }

                // 2) verifica credito
                if (credit == null) credit = BigDecimal.ZERO;
                if (beveragePrice == null) beveragePrice = BigDecimal.ZERO;

                if (credit.compareTo(beveragePrice) < 0) {
                    throw new DaoException("Credito insufficiente.");
                }

                // 3) insert purchase
                psIns.setLong(1, customerId);
                psIns.setLong(2, distributorId);
                psIns.setLong(3, beverageId);
                psIns.setInt(4, sugarQty);
                psIns.setBigDecimal(5, beveragePrice);

                int ins = psIns.executeUpdate();
                if (ins != 1) throw new DaoException("Inserimento purchase fallito (updated=" + ins + ")");

                // 4) update credit
                psUp.setBigDecimal(1, beveragePrice);
                psUp.setLong(2, customerId);

                int upd = psUp.executeUpdate();
                if (upd != 1) throw new DaoException("Update credito fallito (updated=" + upd + ")");

                // 5) read new credit
                psSel.setLong(1, customerId);
                try (ResultSet rs2 = psSel.executeQuery()) {
                    if (!rs2.next()) throw new DaoException("Credito non letto dopo acquisto.");
                    BigDecimal newCredit = rs2.getBigDecimal("credit");
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
            throw new DaoException("Errore DistributorScreenDAO.performPurchase()", e);
        }
    }
}
