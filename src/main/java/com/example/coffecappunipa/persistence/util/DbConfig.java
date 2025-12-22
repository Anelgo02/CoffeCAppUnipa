package com.example.coffecappunipa.persistence.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbConfig {
    private static final String PROPS_FILE = "/application.properties";

    private final String url;
    private final String user;
    private final String password;

    private DbConfig(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static DbConfig load() {
        Properties props = new Properties();

        try (InputStream is = DbConfig.class.getResourceAsStream(PROPS_FILE)) {
            if (is == null) {
                throw new IllegalStateException("File " + PROPS_FILE + " non trovato in classpath.");
            }
            props.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile leggere " + PROPS_FILE, e);
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Proprietà db.* mancanti in application.properties");
        }

        return new DbConfig(url.trim(), user.trim(), password.trim());
    }

    public String getUrl() { return url; }
    public String getUser() { return user; }
    public String getPassword() { return password; }
}