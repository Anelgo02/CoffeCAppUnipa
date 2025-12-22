# Guida al Test - Prototipo Frontend (Assignment 2)

## 1. Panoramica dell'Architettura
Questo progetto rappresenta il **Modulo Frontend** del sistema "Coffee Capp UniPA".
Attualmente, il sistema opera in modalità **"Frontend-Only Simulation"**:
* Non vi è ancora persistenza su Database relazionale.
* Le operazioni di lettura iniziale sono gestite tramite **Fetch API** su file statici (JSON/XML).
* **Persistenza Simulata:** Le operazioni di scrittura (aggiunta/rimozione/modifica) vengono salvate nel **LocalStorage** del browser. Questo garantisce che i dati rimangano consistenti tra le diverse pagine e al ricaricamento della pagina, simulando un vero database.
* **Inizializzazione Dati:** Al primo avvio, l'applicazione popola il LocalStorage leggendo i file statici (`.xml`). Per reinizializzare l'ambiente e tornare ai dati originali, è possibile eseguire il comando `localStorage.clear()` nella **Console del browser (F12)** e ricaricare la pagina.
---

## 2. Istruzioni per l'Esecuzione
1.  Avviare l'applicazione Spring Boot (`CoffeeCappApplication.java`).
2.  Il server risponderà all'indirizzo `http://localhost:8080`.
3.  Tutte le risorse statiche sono servite dalla cartella `src/main/resources/static`.
4.  

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

### B. Portale Cliente (Login e Registrazione)
**Pagina:** `http://localhost:8080/login.html`

> **Nota sull'Implementazione del Login:**
> Rispetto alla richiesta dell'assignment di *reindirizzare alla pagina principale indipendentemente dai dati inseriti*, in questo progetto è stata implementata una logica di **Routing basata sul Ruolo**.
> Avendo optato per una **Single Login Page** (unica pagina di accesso per Clienti, Gestori e Manutentori), è necessario validare le credenziali (sul mock database `users.json` o `localStorage`) per determinare la corretta dashboard di destinazione. Un reindirizzamento incondizionato non avrebbe permesso di distinguere tra le tre tipologie di utente.

* **Credenziali di Test:**
    * **Cliente:** `cliente@unipa.it` / `password123` → Reindirizza a Dashboard Cliente.
    * **Gestore:** `gestore@unipa.it` / `password123` → Reindirizza a Pannello Gestore.
    * **Manutentore:** `manutentore@unipa.it` / `password123` → Reindirizza a Pannello Manutenzione.
* **Test Registrazione (Flusso):**
    * Dalla pagina di login, cliccare su "Registrati qui".
    * Compilare il form con nuovi dati.
    * **Comportamento Atteso:** Poiché la logica è gestita interamente lato client, al click su "Registrati" il sistema salva l'utente nel `localStorage` ed esegue un **reindirizzamento immediato** alla Dashboard Cliente (Auto-Login).
    * *Verifica:* Una volta nella dashboard, effettuare il logout e provare ad accedere con le nuove credenziali appena create: il login avrà successo grazie alla persistenza locale.
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
    * **Modifica Stato (Popup):** Cliccando sul tasto "Stato" nella tabella, si apre un **Modal (Popup)** che permette di cambiare lo stato in *ATTIVO, MANUTENZIONE* o *DISATTIVO*. La modifica è immediata e persistente.

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