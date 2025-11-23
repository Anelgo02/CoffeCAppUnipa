# Guida al Test - Prototipo Frontend (Assignment 2)

## 1. Panoramica dell'Architettura
Questo progetto rappresenta il **Modulo Frontend** del sistema "Coffee Capp UniPA".
Attualmente, il sistema opera in modalità **"Frontend-Only Simulation"**:
* Non vi è ancora persistenza su Database.
* Le operazioni asincrone sono gestite tramite **Fetch API** su file statici (JSON/XML) simulando un Backend.
* La persistenza temporanea (es. credito utente durante la sessione) è gestita tramite **LocalStorage** del browser e variabili in memoria.

---

## 2. Istruzioni per l'Esecuzione
1.  Avviare l'applicazione Spring Boot (`CoffeeCappApplication.java`).
2.  Il server risponderà all'indirizzo `http://localhost:8080`.
3.  Tutte le risorse statiche sono servite dalla cartella `src/main/resources/static`.

---

## 3. Scenari di Test

### A. Schermo Distributore (Simulazione IoT)
**Pagina:** `http://localhost:8080/distributore/index.html`

* **Funzionamento:** La pagina esegue un **Polling** ogni 5 secondi sul file `static/data/connected_user.json`.
* **Test Connessione Utente:**
    1.  Aprire il file `connected_user.json` nell'IDE.
    2.  Impostare `"connected": true`. Entro 5 secondi, lo schermo mostrerà "Marco Rossi" e il credito.
    3.  Impostare `"connected": false`. Lo schermo mostrerà "Nessun utente".
* **Test Acquisto e Persistenza:**
    1.  Selezionare una bevanda e cliccare **"Eroga"**.
    2.  Il credito visualizzato diminuirà.
    3.  **Nota Tecnica:** Il nuovo credito viene salvato nel `localStorage` del browser. Anche se il file JSON originale contiene ancora il credito vecchio, il frontend darà priorità al dato aggiornato localmente, simulando correttamente una sessione persistente.

### B. Portale Cliente (Login e Dashboard)
**Pagina:** `http://localhost:8080/login.html`

* **Login Unificato (Routing):** Il sistema utilizza un file `users.json` come database simulato per reindirizzare l'utente in base al ruolo.
* **Credenziali di Test:**
    * **Cliente:** `cliente@unipa.it` / `password123` → Reindirizza a Dashboard Cliente.
    * **Gestore:** `gestore@unipa.it` / `password123` → Reindirizza a Pannello Gestore.
    * **Manutentore:** `manutentore@unipa.it` / `password123` → Reindirizza a Pannello Manutenzione.
* **Dashboard Cliente:** Mostra i dati dell'utente loggato e permette la simulazione di Ricarica e Connessione (i dati aggiornati sono mantenuti in sessione browser).

### C. Pannello Gestore (Gestione CRUD in RAM)
**Pagina:** `http://localhost:8080/gestore/manutentori.html`

* **Caricamento:** All'avvio, la pagina carica la lista iniziale dal file `manutentori.xml`.
* **Aggiunta/Rimozione:**
    * È possibile aggiungere un nuovo manutentore compilando il form.
    * È possibile rimuovere un manutentore dalla tabella.
* **Nota Importante:** Queste operazioni manipolano l'array di oggetti nella **memoria RAM** di JavaScript.
    * L'interfaccia si aggiorna immediatamente.
    * **Attenzione:** Ricaricando la pagina (F5), le modifiche andranno perse poiché non è possibile scrivere fisicamente sul file XML statico lato server via JavaScript. Questo comportamento è previsto in questa fase del progetto.

### D. Pannello Manutenzione (Lettura XML)
**Pagina:** `http://localhost:8080/manutenzione/index.html`

* Permette di visualizzare lo stato tecnico di un distributore leggendo il file `esempio_stato.xml`.
* **Test:** Inserire ID `UNIPA-001` (Stato Attivo) o `UNIPA-002` (Stato Guasto/Manutenzione) per vedere le differenze nel rendering dei dati.

---

## 4. Struttura Dati (Mock)
I file utilizzati per la simulazione si trovano in `/static/data/`:
* `connected_user.json`: Simula la presenza NFC/Bluetooth al distributore.
* `users.json`: Simula il database utenti per il login.
* `manutentori.xml`: Elenco iniziale addetti.
* `esempio_stato.xml`: Database stato macchine e guasti.