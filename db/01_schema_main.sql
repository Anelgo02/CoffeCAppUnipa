-- ============================================================
-- 1) SCHEMA PRINCIPALE
-- ============================================================

USE coffe_app;

-- 1) Utenti (tutti i ruoli)
CREATE TABLE IF NOT EXISTS users (
                                     id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(120) NULL,
    password_hash VARCHAR(255) NULL,  -- per ora NULL (login non autenticato)
    role          ENUM('CUSTOMER','MAINTAINER','MANAGER') NOT NULL,
    credit        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- 1b) Profilo manutentore (dati anagrafici per XML/gestore)
CREATE TABLE IF NOT EXISTS maintainer_profiles (
                                                   user_id    BIGINT PRIMARY KEY,
                                                   first_name VARCHAR(80)  NOT NULL,
    last_name  VARCHAR(80)  NOT NULL,
    phone      VARCHAR(30)  NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_maint_profile_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- 2) Distributori
CREATE TABLE IF NOT EXISTS distributors (
                                            id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            code          VARCHAR(50) NOT NULL UNIQUE, -- ID che l'utente inserisce (es. "UNIPA-001")
    location_name VARCHAR(120) NULL,
    status        ENUM('ACTIVE','MAINTENANCE','FAULT') NOT NULL DEFAULT 'ACTIVE',
    security_token VARCHAR(100) NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- 3) Stato forniture (1 riga per distributore)
CREATE TABLE IF NOT EXISTS distributor_supplies (
                                                    distributor_id BIGINT PRIMARY KEY,
                                                    coffee_level   INT NOT NULL DEFAULT 0,
                                                    milk_level     INT NOT NULL DEFAULT 0,
                                                    sugar_level    INT NOT NULL DEFAULT 0,
                                                    cups_level     INT NOT NULL DEFAULT 0,
                                                    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                                    CONSTRAINT fk_supplies_distributor
                                                    FOREIGN KEY (distributor_id) REFERENCES distributors(id) ON DELETE CASCADE
    );

-- 4) Guasti / note tecniche (storico)
CREATE TABLE IF NOT EXISTS distributor_faults (
                                                  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  distributor_id BIGINT NOT NULL,
                                                  description    VARCHAR(255) NOT NULL,
    is_open        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at      TIMESTAMP NULL,
    CONSTRAINT fk_faults_distributor
    FOREIGN KEY (distributor_id) REFERENCES distributors(id) ON DELETE CASCADE
    );

-- 5) Connessione cliente -> distributore (al più 1 attiva per cliente)
CREATE TABLE IF NOT EXISTS customer_connections (
                                                    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                    customer_id     BIGINT NOT NULL,
                                                    distributor_id  BIGINT NOT NULL,
                                                    connected_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                    disconnected_at TIMESTAMP NULL,
                                                    CONSTRAINT fk_conn_customer
                                                    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_conn_distributor
    FOREIGN KEY (distributor_id) REFERENCES distributors(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_conn_customer_active
    ON customer_connections(customer_id, disconnected_at);

CREATE INDEX IF NOT EXISTS idx_conn_distributor_active
    ON customer_connections(distributor_id, disconnected_at);

-- 6) Ricariche credito
CREATE TABLE IF NOT EXISTS topups (
                                      id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      customer_id BIGINT NOT NULL,
                                      amount      DECIMAL(10,2) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_topup_customer
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- 7) Bevande (listino)
CREATE TABLE IF NOT EXISTS beverages (
                                         id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         name      VARCHAR(80) NOT NULL UNIQUE,
    price     DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
    );

-- 8) Acquisti (erogazioni)
CREATE TABLE IF NOT EXISTS purchases (
                                         id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         customer_id    BIGINT NOT NULL,
                                         distributor_id BIGINT NOT NULL,
                                         beverage_id    BIGINT NOT NULL,
                                         sugar_qty      INT NOT NULL DEFAULT 0,
                                         price_paid     DECIMAL(10,2) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_customer
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_distributor
    FOREIGN KEY (distributor_id) REFERENCES distributors(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_beverage
    FOREIGN KEY (beverage_id) REFERENCES beverages(id)
    );

-- ------------------------------------------------------------
-- Dati minimi di test (idempotenti)
-- ------------------------------------------------------------

INSERT INTO beverages(name, price) VALUES
                                       ('Espresso', 0.80),
                                       ('Cappuccino', 1.20),
                                       ('Cioccolata', 1.50)
    ON DUPLICATE KEY UPDATE price = VALUES(price);

-- Utenti demo
INSERT INTO users(username, role, credit, email) VALUES
                                                     ('cliente1','CUSTOMER', 5.00, 'cliente1@unipa.it','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lE9lE9lE9lE'),
                                                     ('maint1','MAINTAINER', 0.00, 'maint1@unipa.it', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lE9lE9lE9lE'),
                                                     ('admin1','MANAGER', 0.00, 'admin1@unipa.it', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lE9lE9lE9lE')
    ON DUPLICATE KEY UPDATE role = VALUES(role), credit = VALUES(credit), email = VALUES(email), password_hash = VALUES(password_hash);

-- Profilo manutentore demo (collegato a maint1)
INSERT INTO maintainer_profiles(user_id, first_name, last_name, phone)
SELECT u.id, 'Giuseppe', 'Verdi', '091123456'
FROM users u
WHERE u.username = 'maint1'
    ON DUPLICATE KEY UPDATE first_name = VALUES(first_name), last_name = VALUES(last_name), phone = VALUES(phone);

-- Distributori demo
INSERT INTO distributors(code, location_name, status) VALUES
                                                          ('UNIPA-001','Edificio 1', 'ACTIVE'),
                                                          ('UNIPA-002','Edificio 2', 'MAINTENANCE')
    ON DUPLICATE KEY UPDATE location_name = VALUES(location_name), status = VALUES(status);

-- Supplies: 1 riga per ogni distributore (idempotente)
INSERT INTO distributor_supplies(distributor_id, coffee_level, milk_level, sugar_level, cups_level)
SELECT d.id, 70, 50, 80, 100
FROM distributors d
    ON DUPLICATE KEY UPDATE
                         coffee_level = VALUES(coffee_level),
                         milk_level   = VALUES(milk_level),
                         sugar_level  = VALUES(sugar_level),
                         cups_level   = VALUES(cups_level);
