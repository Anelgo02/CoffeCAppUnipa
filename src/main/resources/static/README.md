# Coffee Capp UniPA – Progetto Principale

Questo repository contiene il **progetto principale** del sistema *Coffee Capp UniPA*.
Il progetto implementa il **backend applicativo centrale** e il **frontend web** per la gestione di una rete di distributori automatici di bevande, con supporto a più ruoli, persistenza su database e integrazione con un servizio di monitoraggio esterno.

Il focus di questo progetto è:
- gestione utenti e ruoli
- gestione distributori e scorte
- simulazione utilizzo del distributore
- coordinamento applicativo e logica di business

---

## 1. Struttura Generale del Progetto

Il progetto è strutturato come una **web application Jakarta EE** basata su Servlet, con risorse statiche HTML/JS/CSS.

Struttura logica principale:

- `web.servlet` → Servlet HTTP (API backend)
- `persistence.dao` → Accesso al database (DAO JDBC)
- `web.monitor` → Client HTTP verso il servizio di monitoraggio
- `static/` → Frontend (HTML, CSS, JavaScript)

---

## 2. Servlet Backend (API)

### 🔹 Autenticazione e Routing
- **RoutingServlet**
    - Gestisce login, logout e reindirizzamento in base al ruolo
    - Imposta il ruolo in sessione
    - È il punto centrale di controllo degli accessi

---

### 🔹 Customer (Cliente)

- **CustomerConnectServlet**
    - Connette un cliente a un distributore
    - Imposta lo stato di connessione lato backend

- **CustomerDisconnectServlet**
    - Disconnette il cliente dal distributore

- **CustomerDashboardServlet**
    - Fornisce i dati del cliente loggato (username, credito)

---

### 🔹 Distributor (Simulazione Distributore)

- **DistributorPollServlet**
    - Endpoint di polling usato dallo schermo del distributore
    - Verifica se un cliente è connesso
    - Restituisce username e credito

- **DistributorBeveragesServlet**
    - Restituisce la lista delle bevande disponibili

- **DistributorPurchaseServlet**
    - Gestisce l’erogazione della bevanda
    - Scala il credito
    - Aggiorna le scorte

---

### 🔹 Manager (Gestore)

- **ManagerDistributorsServlet**
    - Lista distributori
    - Creazione distributori
    - Eliminazione distributori
    - Cambio stato (attivo / manutenzione / disattivo)

- **ManagerMaintainersServlet**
    - Lista manutentori
    - Creazione manutentori
    - Eliminazione manutentori

- **MonitorSyncServlet**
    - Sincronizza tutti i distributori presenti nel DB principale
    - Legge i dati **direttamente dal database**
    - Espone un endpoint invocabile dal pannello Manager

---

### 🔹 Maintainer (Manutentore)

- **MaintainerDistributorsServlet**
    - Rifornimento scorte dei distributori
    - Cambio stato distributori
    - Operazioni tecniche
    - Aggiorna automaticamente anche il servizio di monitoraggio

---

## 3. DAO e Persistenza

Tutta la persistenza è gestita tramite **DAO JDBC**, senza ORM.

Principali DAO:
- `UserDAO`
- `DistributorDAO`
- `BeverageDAO`
- `ManagerReadDAO`

Ogni DAO:
- apre connessioni JDBC esplicite
- esegue query SQL dirette
- solleva `DaoException` in caso di errore

---

## 4. Frontend – Risorse Statiche

Le risorse frontend si trovano in `src/main/resources/static`.

### 🔹 JavaScript principali

- **apiHelpers.js**
    - Wrapper per `fetch`
    - Gestione errori
    - Uniforma chiamate GET / POST

- **manager.main.js**
    - Pannello Gestore
    - Caricamento distributori
    - Filtri e ricerca
    - Modal cambio stato
    - Avvio sincronizzazione monitor

- **distributor.poll.js**
    - Simulazione schermo distributore
    - Polling stato cliente
    - Acquisto bevande
    - Invio heartbeat periodico

---

### 🔹 HTML principali

- `login.html`
- `gestore/index.html`
- `manutenzione/index.html`
- `distributore/index.html`
- `cliente/index.html`

Ogni pagina:
- usa fetch verso le servlet backend
- non contiene logica applicativa critica
- delega tutto al backend

---

## 5. Gestione Ruoli e Sessioni

Il progetto utilizza:
- sessione HTTP standard
- attributo `SESSION_ROLE`
- controllo ruolo **in ogni servlet**

Nessun endpoint critico è accessibile senza ruolo corretto.

---

## 6. Sincronizzazione Stato Distributori

Quando:
- un distributore viene creato
- viene eliminato
- cambia stato
- viene rifornito

Il backend principale:
- aggiorna il proprio database
- invia **best-effort** l’aggiornamento al servizio di monitoraggio

In aggiunta, è disponibile una **sincronizzazione completa manuale** tramite servlet dedicata.
