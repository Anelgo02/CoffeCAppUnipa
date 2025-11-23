# Guida al Test - Prototipo Frontend (Assignment 2)

## 1. Panoramica dell'Architettura
Questo progetto rappresenta il **Modulo Frontend** del sistema "Coffee Capp UniPA".
Attualmente, il sistema opera in modalità **"Frontend-Only Simulation"**:
* Non vi è ancora persistenza su Database relazionale.
* Le operazioni di lettura iniziale sono gestite tramite **Fetch API** su file statici (JSON/XML).
* **Persistenza Simulata:** Le operazioni di scrittura (aggiunta/rimozione/modifica) vengono salvate nel **LocalStorage** del browser. Questo garantisce che i dati rimangano consistenti tra le diverse pagine e al ricaricamento della pagina, simulando un vero database.

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
    3.  **Nota Tecnica:** Il nuovo credito viene salvato nel `localStorage` del browser. Anche se il file JSON originale contiene ancora il credito vecchio, il frontend darà priorità al dato aggiornato localmente.

### B. Portale Cliente (Login e Dashboard)
**Pagina:** `http://localhost:8080/login.html`

* **Login Unificato (Routing):** Il sistema utilizza un file `users.json` come database simulato per reindirizzare l'utente in base al ruolo.
* **Credenziali di Test:**
    * **Cliente:** `cliente@unipa.it` / `password123` → Reindirizza a Dashboard Cliente.
    * **Gestore:** `gestore@unipa.it` / `password123` → Reindirizza a Pannello Gestore.
    * **Manutentore:** `manutentore@unipa.it` / `password123` → Reindirizza a Pannello Manutenzione.
* **Dashboard Cliente:** Mostra i dati dell'utente loggato e permette la simulazione di Ricarica e Connessione (i dati aggiornati sono mantenuti in sessione browser).

### C. Pannello Gestore (CRUD Completo con LocalStorage)
**Pagina:** `http://localhost:8080/gestore/index.html`

Questa sezione è stata rifattorizzata per simulare un'applicazione gestionale completa.
* **Caricamento Dati:** Al primo avvio, i dati vengono letti dai file XML (`manutentori.xml`, `esempio_stato.xml`). Successivamente, vengono letti dal `localStorage` per mantenere le modifiche.
* **Gestione Manutentori:**
    * Visualizzazione tabellare con opzione "Elimina".
    * **Aggiunta:** Cliccando su "+ Nuovo Manutentore" si viene reindirizzati a una pagina dedicata (`aggiungi_manutentore.html`). Dopo il salvataggio, si ritorna automaticamente alla lista aggiornata.
* **Gestione Distributori:**
    * Visualizzazione tabellare con stato colorato (Verde/Giallo/Rosso).
    * **Aggiunta:** Cliccando su "+ Nuovo Distributore" si accede alla pagina di inserimento dedicata (`aggiungi_distributore.html`).
    * **Modifica Stato (Popup):** Cliccando sul tasto "Stato" nella tabella, si apre un **Modal (Popup)** che permette di cambiare lo stato in *ATTIVO, IN MANUTENZIONE* o *FUORI SERVIZIO*. La modifica è immediata e persistente.

### D. Pannello Manutenzione (Lettura XML)
**Pagina:** `http://localhost:8080/manutenzione/index.html`

* Permette di visualizzare lo stato tecnico dettagliato di un distributore (livelli, guasti) leggendo il file `esempio_stato.xml`.
* **Test:** Inserire ID `UNIPA-001` (Stato Attivo) o `UNIPA-002` (Stato Guasto/Manutenzione) per vedere le differenze nel rendering dei dati.

---

## 4. Struttura Dati (Mock)
I file utilizzati per la simulazione si trovano in `/static/data/`:
* `connected_user.json`: Simula la presenza NFC/Bluetooth al distributore.
* `users.json`: Simula il database utenti per il login.
* `manutentori.xml`: Elenco iniziale addetti (usato per il primo caricamento).
* `esempio_stato.xml`: Database stato macchine e guasti (usato per il primo caricamento).