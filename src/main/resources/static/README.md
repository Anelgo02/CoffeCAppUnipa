# CoffeeCApp UniPA — Progetto Principale (Backend + Frontend)

Questo repository contiene **Backend (Spring Boot)** + **Frontend (SPA)** del sistema **CoffeeCApp UniPA**.

Il progetto implementa un’architettura **Web/IoT ibrida** per la gestione di una rete di distributori automatici intelligenti, simulando scenari realistici di:
- telemetria e heartbeat
- pagamenti e concorrenza (race conditions)
- manutenzione remota
- sincronizzazione con un servizio esterno di monitoraggio (**CoffeeMonitor**)

---

## Obiettivi (in pratica)
- **Separare nettamente** client (SPA) e server (API) senza rendering lato server.
- Usare **Servlet standard** (Jakarta EE) come controller, dentro un container Spring Boot.
- Persistenza **JDBC puro + DAO**, con controllo totale su SQL e transazioni.
- Security “ibrida”: **utenti umani** con Spring Security + **dispositivi IoT** con validazione applicativa.

---

## Architettura e Stack Tecnologico

### Backend
- **Java + Spring Boot** (come container per **Servlet Jakarta EE**)
- **Pattern MVC** (controller = servlet, model = DAO/entità, view = SPA statica)
- **Persistenza: DAO + JDBC puro**
    - **Niente ORM (Hibernate/JPA)**: scelta intenzionale per:
        - controllo assoluto sulle query SQL
        - performance e prevenzione N+1 (batch loading dove necessario)
        - gestione manuale delle transazioni **ACID** (commit/rollback) in operazioni critiche (es. pagamenti)

### Frontend
- **Single Page Application** in:
    - HTML5
    - CSS3
    - JavaScript Vanilla
- Comunicazione con backend tramite **Fetch API**
- Persistenza locale tramite **localStorage** (fondamentale per simulare la memoria “flash” dei dispositivi IoT)

### Sicurezza e integrazione
- **Spring Security**: autenticazione, autorizzazione (RBAC), protezione CSRF
- **CoffeeMonitor** (servizio esterno su porta 8081):
    - comunicazione HTTP sincrona
    - pattern **Proxy** (per CORS/timeout/degrado controllato)
    - pattern **Dual Write** (best-effort) sulle operazioni amministrative

---

## 🔐 Security Layer (Strategia Ibrida)

Qui la differenza è netta: **utenti** ≠ **dispositivi**.

### 1) Autenticazione Utenti (User-Centric)
Gestita da **Spring Security** per:
- Cliente (`ROLE_CUSTOMER`)
- Manutentore (`ROLE_MAINTAINER`)
- Gestore/Manager (`ROLE_MANAGER`)

Caratteristiche:
- **Meccanismo**: form login classico (`/login.html`)
- **Password**: hashing **BCrypt**
- **Sessione**: stateful (`JSESSIONID`)
- **Legacy bridge**: filtro custom `LegacySessionBridgeFilter` che inietta `Principal/Role` nella `HttpSession` standard (compatibilità con servlet preesistenti)
- **CSRF**: attivo con `CsrfCookieFilter`
    - espone il token nel cookie `XSRF-TOKEN`
    - la SPA lo rimanda nelle richieste `POST` (tipicamente header `X-XSRF-TOKEN`)

### 2) Autenticazione Dispositivi (Device-Centric) — Token + Spring Security

I distributori operano in modalità kiosk (unattended): niente credenziali umane.

La sicurezza del canale IoT è implementata con un **token di dispositivo** gestito dal backend:

- **Boot pubblico**: `POST /api/distributor/boot` è `permitAll` e **genera** un token unico.
- **API operative protette**: le chiamate successive (`poll`, `beverages`, `purchase`) richiedono
  l'header `X-Distributor-Auth: <token>`.
- Un filtro Spring Security custom (`DistributorTokenFilter`) intercetta l'header, valida il token nel DB e,
  se valido, crea un’`Authentication` con ruolo `ROLE_DISTRIBUTOR`.

Quindi:
- **solo l’endpoint di boot è pubblico**
- le API operative sono **protette da Spring Security** tramite `hasRole("DISTRIBUTOR")`

#### Flusso di sicurezza IoT (step-by-step)

1) Boot (prima inizializzazione)
- UI `boot.html` invia `POST /api/distributor/boot?code=UNIPA-001`
- Backend verifica che il distributore esista
- Backend genera `security_token` e lo salva nel DB
- Frontend salva:
    - `distributor_identity` (code)
    - `distributor_token` (token)

2) API operative
- Ogni chiamata successiva invia `X-Distributor-Auth: <token>`
- `DistributorTokenFilter`:
    - legge l’header
    - valida token nel DB
    - inserisce nel `SecurityContext` un’identity “virtuale” con `ROLE_DISTRIBUTOR`
- Le route `poll/beverages/purchase` sono autorizzate solo se `hasRole("DISTRIBUTOR")`

---

#### Policy Anti-Reboot (no clonazione / no doppia inizializzazione)

La servlet di boot **non rigenera** un token se il distributore risulta già inizializzato:
- Se nel DB è già presente un `security_token` non nullo → risposta `409 Conflict`
- Motivazione: evitare che due kiosk diversi possano usare lo stesso codice distributore.

---

## 🤖 Ciclo di Vita del Distributore (Simulazione IoT)

Il software del distributore (`/distributore/index.html`) implementa una **macchina a stati finiti**.
La creazione dei distributori può essere effettuata solamente dall'admin,
questa scelta consente l'attivazione tramite ID del distributore che è già stato aggiunto nel DB.
### Boot (primo avvio)
- Uno script “guardiano” controlla `localStorage`
- Se l’identità macchina non esiste → redirect forzato a `boot.html`
- Il tecnico inserisce il codice macchina (es. `UNIPA-001`)
- Il backend valida e registra l’attivazione

### Standby (idle mode)
- Loop di **polling** verso il server (es. ogni 3s)
- UI mostra schermata di attesa: “Connettiti con l’app”
- Invio periodico di **heartbeat** al servizio esterno CoffeeMonitor

### Operatività (active mode)
- Quando un cliente si connette con l’app, il polling rileva la sessione attiva
- L’interfaccia si sblocca: credito utente + listino bevande
- **Transazione sicura**:
    - pagamento con **lock pessimistico** (`SELECT ... FOR UPDATE`)
    - prevenzione race condition (es. doppia erogazione / disconnessione durante pagamento)

>Nota: per testare diversi distributori da browser bisogna usare il bottone resetID per eliminare
> dalla memoria del kiosk l'identita' del distributore e eliminare dal db il token di sicurezza.
---

## 📡 Integrazione con CoffeeMonitor (porta 8081)

Funzioni principali:
- **Proxy Map**: `GET /api/monitor/map`
    - il backend fa da proxy per bypassare CORS e gestire timeout/errori (degrado controllato)
- **Proxy Heartbeat**: `POST /monitor/heartbeat`
    - inoltra heartbeat dei distributori
- **Dual Write (Best Effort)**:
    - provisioning distributore, cambio stato, ecc.
    - scrittura su DB locale + chiamata al servizio remoto (con gestione fallimenti)
- **Sync**: `POST /api/monitor/sync`
    - riconciliazione in caso di disallineamento

---

## 🔌 API Endpoints (Servlet)

> Nota: gli endpoint “Area Distributore” sono **pubblici a livello Spring Security** ma protetti con logica applicativa (validazione ID + stato macchina).

### Area Gestore (`ROLE_MANAGER`)
- `GET  /api/manager/maintainers.xml` — Export XML staff (generazione manuale via `StringBuilder`)
- `GET  /api/manager/maintainers/list` — Lista staff (JSON)
- `POST /api/manager/maintainers/create` — Assunzione staff (transazione multi-tabella)
- `POST /api/manager/maintainers/delete` — Licenziamento staff
- `GET  /api/manager/distributors/list` — Lista distributori (merge dati DB + Monitor)
- `POST /api/manager/distributors/create` — Provisioning nuova macchina
- `POST /api/manager/distributors/delete` — Rimozione macchina
- `POST /api/manager/distributors/status` — Cambio stato forzato
- `POST /api/monitor/sync` — Sync forzata DB ↔ Monitor

### Area Manutentore (`ROLE_MAINTAINER`)
- `GET  /api/maintainer/me` — Info sessione
- `POST /api/maintainer/distributors/refill` — Ripristino scorte (full refill)
- `POST /api/maintainer/distributors/status` — Cambio stato operativo

### Area Cliente (`ROLE_CUSTOMER`)
- `POST /api/customer/register` — Registrazione (hash password + auto-login)
- `GET  /api/customer/me` — Profilo + credito
- `POST /api/customer/topup` — Ricarica credito (transazionale, no-cache)
- `POST /api/customer/connect` — Handshake con distributore (check manutenzione/guasto)
- `POST /api/customer/disconnect` — Chiusura sessione
- `GET  /api/customer/current-connection` — Stato connessione corrente

### Area Distributore (IoT — pubbliche)
- `POST /api/distributor/boot` — Inizializzazione hardware
- `GET  /api/distributor/poll` — Check presenza cliente (polling)
- `GET  /api/distributor/beverages` — Listino prezzi
- `POST /api/distributor/purchase` — Erogazione bevanda (transazione critica)
- `POST /api/distributor/reset` — Reset Token per il distributore

---

## 📂 Struttura del Progetto

- `src/main/java/.../web/servlet` → **Controller** (routing + logica web)
- `src/main/java/.../persistence/dao` → **Data Access Layer** (SQL, transazioni, mapping)
- `src/main/java/.../security` → Spring Security config + filtri custom
- `src/main/java/.../web/monitor` → HTTP client / proxy verso CoffeeMonitor
- `src/main/resources/static` → **Frontend** (HTML/JS/CSS)
- `src/main/java/.../util` → **Utility**(File di configurazione e utility)

---

## 🚀 Guida Rapida all’Avvio

### Prerequisiti
- Java (versione coerente col progetto)
- Maven/Gradle (in base al build usato)
- MySQL (o DB relazionale equivalente configurato)
- Tomcat (o container equivalente) per CoffeeMonitor

### 1) Database
- Assicurati che il DB sia attivo
- Configura credenziali e URL nel DBMS utilizzando quelle di `application.properties`
- Importa lo schema SQL del progetto che si trova nella cartella `db`

### 2) Avvia CoffeeMonitor (servizio esterno)
- Deploy su Tomcat
- Porta tipica: **8081**
- Verifica che l’endpoint di heartbeat/map risponda

### 3) Avvia la Main App (CoffeeCApp)
- Avvia `CoffeeCappApplication`
- Porta tipica: **8080**
- Apri: `http://localhost:8080/login.html`

---

## 🧪 Setup iniziale consigliato (flusso reale)
1. Login come **Gestore**
2. Crea un distributore (es. `UNIPA-001`)
3. Apri il distributore in una nuova finestra:
    - `http://localhost:8080/distributore/index.html`
4. Se è “vergine”, verrai rediretto a `boot.html`
5. Inserisci l’ID macchina (`UNIPA-001`) e completa l’attivazione
6. Ora il distributore entra in standby e inizia polling + heartbeat
7. Login come **Cliente** e prova:
    - top-up credito
    - connect → acquisto → disconnect

---


## Licenza
Progetto universitario/didattico.