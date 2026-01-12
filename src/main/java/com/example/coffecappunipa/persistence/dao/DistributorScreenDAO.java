package com.example.coffecappunipa.persistence.dao;

import com.example.coffecappunipa.persistence.util.DaoException;
import com.example.coffecappunipa.persistence.util.DbConnectionManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class DistributorScreenDAO {

    // Error codes STABILI (da mappare nella servlet)
    public static final String ERR_NO_CUSTOMER_CONNECTED = "ERR_NO_CUSTOMER_CONNECTED";
    public static final String ERR_INSUFFICIENT_CREDIT   = "ERR_INSUFFICIENT_CREDIT";
    public static final String ERR_INVALID_BEVERAGE      = "ERR_INVALID_BEVERAGE";
    public static final String ERR_INVALID_PRICE         = "ERR_INVALID_PRICE";
    public static final String ERR_OUT_OF_STOCK          = "ERR_OUT_OF_STOCK";

    // Consumi per una erogazione (coerenti con distributor_supplies attuale)
    // Modifica qui se vuoi:
    private static final int COFFEE_GR_PER_PURCHASE = 7;
    private static final int MILK_ML_PER_PURCHASE   = 0;
    private static final int CUPS_PER_PURCHASE      = 1;

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
     * Erogazione (robusta + scalo scorte):
     * - lock connessione attiva del distributore (FOR UPDATE)
     * - legge prezzo bevanda dal DB (in transazione)
     * - scala scorte in modo atomico (UPDATE con condizioni >=)
     * - scala credito con update condizionale (credit >= price)
     * - inserisce purchase
     * - ritorna nuovo credito
     */
    public BigDecimal performPurchase(String distributorCode, long beverageId, int sugarQty) {

        if (sugarQty < 0) sugarQty = 0;
        if (sugarQty > 10) sugarQty = 10;

        // 1) Lock connessione attiva + ricavo customerId/distributorId
        //impedisce che l'utente si disconnetta mentre performa l'acquisto
        String lockConnSql =
                "SELECT u.id AS customer_id, d.id AS distributor_id " +
                        "FROM customer_connections cc " +
                        "JOIN distributors d ON d.id = cc.distributor_id " +
                        "JOIN users u ON u.id = cc.customer_id " +
                        "WHERE d.code = ? AND cc.disconnected_at IS NULL " +
                        "ORDER BY cc.connected_at DESC " +
                        "LIMIT 1 " +
                        "FOR UPDATE";

        // 2) Leggo prezzo dal DB (evito parametri esterni)
        String selectBeverageSql =
                "SELECT price " +
                        "FROM beverages " +
                        "WHERE id = ? AND is_active = 1";

        // 3) Scalo scorte (atomico: non deve andare sotto zero)
        String updateSuppliesAtomicSql =
                "UPDATE distributor_supplies " +
                        "SET coffee_level = coffee_level - ?, " +
                        "    milk_level   = milk_level   - ?, " +
                        "    sugar_level  = sugar_level  - ?, " +
                        "    cups_level   = cups_level   - ? " +
                        "WHERE distributor_id = ? " +
                        "  AND coffee_level >= ? " +
                        "  AND milk_level   >= ? " +
                        "  AND sugar_level  >= ? " +
                        "  AND cups_level   >= ?";

        // 4) Update credito atomico
        String updateCreditAtomicSql =
                "UPDATE users " +
                        "SET credit = credit - ? " +
                        "WHERE id = ? AND credit >= ?";

        // 5) Insert purchase
        String insertPurchaseSql =
                "INSERT INTO purchases(customer_id, distributor_id, beverage_id, sugar_qty, price_paid) " +
                        "VALUES(?, ?, ?, ?, ?)";

        // 6) Leggo nuovo credito
        String selectNewCreditSql =
                "SELECT credit FROM users WHERE id = ?";

        Connection conn = null;

        try {
            conn = DbConnectionManager.getConnection();
            conn.setAutoCommit(false);

            long customerId;
            long distributorId;

            // 1) lock connessione
            try (PreparedStatement psLock = conn.prepareStatement(lockConnSql)) {
                psLock.setString(1, distributorCode);

                try (ResultSet rs = psLock.executeQuery()) {
                    if (!rs.next()) throw new DaoException(ERR_NO_CUSTOMER_CONNECTED);
                    customerId = rs.getLong("customer_id");
                    distributorId = rs.getLong("distributor_id");
                }
            }

            // 2) prezzo bevanda dal DB
            BigDecimal price;
            try (PreparedStatement psBev = conn.prepareStatement(selectBeverageSql)) {
                psBev.setLong(1, beverageId);
                try (ResultSet rs = psBev.executeQuery()) {
                    if (!rs.next()) throw new DaoException(ERR_INVALID_BEVERAGE);
                    price = rs.getBigDecimal("price");
                }
            }

            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new DaoException(ERR_INVALID_PRICE);
            }

            // 3) scalo scorte (prima o dopo il credito è indifferente: rollback copre tutto)
            int coffeeNeed = COFFEE_GR_PER_PURCHASE;
            int milkNeed   = MILK_ML_PER_PURCHASE;
            int sugarNeed  = sugarQty;
            int cupsNeed   = CUPS_PER_PURCHASE;

            try (PreparedStatement psSup = conn.prepareStatement(updateSuppliesAtomicSql)) {
                psSup.setInt(1, coffeeNeed);
                psSup.setInt(2, milkNeed);
                psSup.setInt(3, sugarNeed);
                psSup.setInt(4, cupsNeed);
                psSup.setLong(5, distributorId);

                psSup.setInt(6, coffeeNeed);
                psSup.setInt(7, milkNeed);
                psSup.setInt(8, sugarNeed);
                psSup.setInt(9, cupsNeed);

                int updSup = psSup.executeUpdate();
                if (updSup != 1) {
                    throw new DaoException(ERR_OUT_OF_STOCK);
                }
            }

            // 4) update credito atomico
            try (PreparedStatement psUp = conn.prepareStatement(updateCreditAtomicSql)) {
                psUp.setBigDecimal(1, price);
                psUp.setLong(2, customerId);
                psUp.setBigDecimal(3, price);

                int upd = psUp.executeUpdate();
                if (upd != 1) throw new DaoException(ERR_INSUFFICIENT_CREDIT);
            }

            // 5) insert purchase
            try (PreparedStatement psIns = conn.prepareStatement(insertPurchaseSql)) {
                psIns.setLong(1, customerId);
                psIns.setLong(2, distributorId);
                psIns.setLong(3, beverageId);
                psIns.setInt(4, sugarQty);
                psIns.setBigDecimal(5, price);

                int ins = psIns.executeUpdate();
                if (ins != 1) throw new DaoException("ERR_PURCHASE_INSERT_FAILED");
            }

            // 6) read new credit
            BigDecimal newCredit;
            try (PreparedStatement psSel = conn.prepareStatement(selectNewCreditSql)) {
                psSel.setLong(1, customerId);
                try (ResultSet rs2 = psSel.executeQuery()) {
                    if (!rs2.next()) throw new DaoException("ERR_CREDIT_READ_FAILED");
                    newCredit = rs2.getBigDecimal("credit");
                }
            }

            conn.commit();
            return newCredit;

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            if (e instanceof DaoException) throw (DaoException) e;
            throw new DaoException("Errore DistributorScreenDAO.performPurchase()", e);

        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
