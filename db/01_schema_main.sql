USE coffe_app;

-- 1) Utenti (tutti i ruoli)
CREATE TABLE users (
                       id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                       username     VARCHAR(50)  NOT NULL UNIQUE,
                       email        VARCHAR(120) NULL,
                       password_hash VARCHAR(255) NULL,  -- per ora puoi lasciarlo NULL (login non autenticato)
                       role         ENUM('CUSTOMER','MAINTAINER','MANAGER') NOT NULL,
                       credit       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                       created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2) Distributori
CREATE TABLE distributors (
                              id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                              code          VARCHAR(50) NOT NULL UNIQUE, -- ID che l'utente inserisce (es. "D-001")
                              location_name VARCHAR(120) NULL,
                              status        ENUM('ACTIVE','MAINTENANCE','FAULT') NOT NULL DEFAULT 'ACTIVE',
                              created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3) Stato forniture (1 riga per distributore)
CREATE TABLE distributor_supplies (
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
CREATE TABLE distributor_faults (
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
CREATE TABLE customer_connections (
                                      id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      customer_id    BIGINT NOT NULL,
                                      distributor_id BIGINT NOT NULL,
                                      connected_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      disconnected_at TIMESTAMP NULL,
                                      CONSTRAINT fk_conn_customer
                                          FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
                                      CONSTRAINT fk_conn_distributor
                                          FOREIGN KEY (distributor_id) REFERENCES distributors(id) ON DELETE CASCADE
);

CREATE INDEX idx_conn_customer_active ON customer_connections(customer_id, disconnected_at);
CREATE INDEX idx_conn_distributor_active ON customer_connections(distributor_id, disconnected_at);

-- 6) Ricariche credito
CREATE TABLE topups (
                        id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                        customer_id BIGINT NOT NULL,
                        amount      DECIMAL(10,2) NOT NULL,
                        created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_topup_customer
                            FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 7) Bevande (listino)
CREATE TABLE beverages (
                           id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                           name      VARCHAR(80) NOT NULL UNIQUE,
                           price     DECIMAL(10,2) NOT NULL,
                           is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 8) Acquisti (erogazioni)
CREATE TABLE purchases (
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

-- Dati minimi di test
INSERT INTO beverages(name, price) VALUES
                                       ('Espresso', 0.80),
                                       ('Cappuccino', 1.20),
                                       ('Cioccolata', 1.50);

INSERT INTO users(username, role, credit) VALUES
                                              ('cliente1','CUSTOMER', 5.00),
                                              ('maint1','MAINTAINER', 0.00),
                                              ('admin1','MANAGER', 0.00);

INSERT INTO distributors(code, location_name, status) VALUES
                                                          ('D-001','Edificio 1', 'ACTIVE'),
                                                          ('D-002','Edificio 2', 'MAINTENANCE');

INSERT INTO distributor_supplies(distributor_id, coffee_level, milk_level, sugar_level, cups_level)
SELECT id, 70, 50, 80, 100 FROM distributors;