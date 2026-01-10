# CoffeeCApp UniPA — Progetto Principale (Backend + Frontend)

Questo repository contiene il **progetto principale** del sistema *CoffeeCApp UniPA*.

L’applicazione implementa:

* **Backend applicativo centrale** (Servlet Jakarta EE + DAO JDBC)
* **Frontend web** (pagine statiche HTML/CSS/JS)
* **Autenticazione e autorizzazione** tramite **Spring Security**
* Integrazione con servizio esterno **CoffeeMonitor** (WAR su Tomcat) tramite **proxy** dal backend principale

---

## Stack Tecnologico

* Java
* Servlet (Jakarta EE)
* Spring Security
* JDBC DAO (senza ORM)
* Frontend: HTML + JavaScript (fetch/AJAX)

---

## Struttura del Progetto

* `src/main/java/.../web/servlet` → Servlet HTTP (API backend)
* `src/main/java/.../persistence/dao` → DAO JDBC
* `src/main/java/.../security` → Configurazione Spring Security + filtri (bridge legacy)
* `src/main/resources/static` → Frontend statico (HTML/CSS/JS)

---

## Autenticazione e Sicurezza

### Login / Logout (Spring Security)

* Login page: `GET /login.html`
* Login submit: `POST /auth/login`
* Logout: `POST /auth/logout`

### Ruoli

* `ROLE_CUSTOMER`
* `ROLE_MAINTAINER`
* `ROLE_MANAGER`

### CSRF + AJAX

La protezione CSRF è attiva.

* Il token viene esposto come cookie `XSRF-TOKEN`.
* Le richieste `POST` via AJAX devono inviare l’header: `X-XSRF-TOKEN: <token>`.

Gli helper JS (`apiHelpers.js`) gestiscono automaticamente l’invio del CSRF token sulle richieste `POST`.

---

## Integrazione con CoffeeMonitor (WAR su Tomcat)

CoffeeMonitor gira su un Tomcat separato (porta tipica **8081**) e fornisce lo stato runtime dei distributori.

Per evitare problemi di **CORS** (8080 → 8081), il progetto principale usa un **proxy server-to-server**:

* Proxy nel progetto principale:
    * `GET /api/monitor/map` → il backend chiama CoffeeMonitor e restituisce lo stesso JSON al frontend.

> **Nota:** l’URL interno del monitor (esempio in locale) è:
> `http://localhost:8081/CoffeeMonitor_war_exploded/api/monitor/map`
> (può variare in base al deploy/context path su Tomcat).

---

## Endpoint

### Routing (legacy redirect)

**RoutingServlet**

* `POST /route/login`
* `POST /route/register`
* `GET|POST /route/logout`

> Queste route gestiscono redirect/compatibilità legacy e sessioni “storiche”. L’autenticazione vera è gestita da Spring Security.

### Customer (Cliente)

**CustomerServlet**

* `POST /api/customer/register` *(public)*
* `GET /api/customer/get` *(ROLE_CUSTOMER)*
* `GET /api/customer/me` *(ROLE_CUSTOMER)*

**CustomerConnectionServlet**

* `POST /api/customer/connect` *(ROLE_CUSTOMER)*
* `POST /api/customer/disconnect` *(ROLE_CUSTOMER)*
* `GET /api/customer/current-connection` *(ROLE_CUSTOMER)*

**CustomerTopUpServlet**

* `POST /api/customer/topup` *(ROLE_CUSTOMER)*

### Distributor (Schermo Distributore)

**DistributorScreenServlet**

* `GET /api/distributor/poll` *(public)*
* `GET /api/distributor/beverages` *(public)*
* `POST /api/distributor/purchase` *(ROLE_CUSTOMER)*

**MonitorProxyServlet**

* `POST /monitor/heartbeat` *(public)*

### Maintainer (Manutentore)

**MaintainerServlet**

* `GET /api/maintainer/me` *(ROLE_MAINTAINER)*

**MaintainerDistributorsServlet**

* `POST /api/maintainer/distributors/refill` *(ROLE_MAINTAINER)*
* `POST /api/maintainer/distributors/status` *(ROLE_MAINTAINER)*

### Manager (Gestore)

**ManagerServlet**

* `GET /api/manager/maintainers.xml` *(ROLE_MANAGER)*
* `GET /api/manager/maintainers/list` *(ROLE_MANAGER)*
* `POST /api/manager/maintainers/create` *(ROLE_MANAGER)*
* `POST /api/manager/maintainers/delete` *(ROLE_MANAGER)*
* `GET /api/manager/distributors/list` *(ROLE_MANAGER)*
* `POST /api/manager/distributors/create` *(ROLE_MANAGER)*
* `POST /api/manager/distributors/delete` *(ROLE_MANAGER)*
* `POST /api/manager/distributors/status` *(ROLE_MANAGER)*

**MonitorSyncServlet**

* `POST /api/monitor/sync` *(ROLE_MANAGER)*

### XML Stato distributori (DB + Monitor)

**DistributorsStateXmlServlet**

* `GET /api/distributors/state.xml` *(ROLE_MANAGER / ROLE_MAINTAINER)*

### Proxy verso CoffeeMonitor

**MonitorMapProxyServlet**

* `GET /api/monitor/map` *(ROLE_MANAGER / ROLE_MAINTAINER)*

---

## Frontend (Static)

Directory: `src/main/resources/static`

**Pagine principali:**
* `login.html`
* `cliente/index.html`
* `manutenzione/index.html`
* `gestore/index.html`
* `distributore/index.html`

**JavaScript principali:**
* `js/apiHelpers.js` → wrapper `fetch` + CSRF + gestione errori 401/403
* `js/client.main.js` → dashboard cliente
* `js/maintainer.main.js` → dashboard manutentore
* `js/manager.main.js` → dashboard gestore (include overlay stati monitor via `/api/monitor/map`)
* `js/distributor.poll.js` → polling schermo distributore

---

## Avvio in Locale (dev)

### 1) Avvia il progetto principale (porta 8080)

* Run dell’app principale (Spring Boot / embedded Tomcat).
* Apri: `http://localhost:8080/login.html`

### 2) Avvia CoffeeMonitor (porta 8081)

* Deploy WAR su Tomcat esterno.
* Verifica: `http://localhost:8081/CoffeeMonitor_war_exploded/api/monitor/map` (o context equivalente).

### 3) Verifica proxy dal progetto principale

* Verificare che il backend principale riesca a comunicare con il monitor:
* `http://localhost:8080/api/monitor/map`
