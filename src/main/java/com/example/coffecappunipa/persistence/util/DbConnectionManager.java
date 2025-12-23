package com.example.coffecappunipa.persistence.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnectionManager {

    private static final DbConfig CONFIG = DbConfig.load();

    private DbConnectionManager() {}

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    CONFIG.getUrl(),
                    CONFIG.getUser(),
                    CONFIG.getPassword()
            );
        } catch (SQLException e) {
            throw new DaoException("Errore connessione DB (JDBC). Controlla db.url/db.user/db.password.", e);
        }
    }
}
