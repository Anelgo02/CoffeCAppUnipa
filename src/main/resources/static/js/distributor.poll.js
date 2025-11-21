

const CONNECTED_URL = "../data/connected_user.json"; // path provvisorio

let currentUser = null;
let messageTimeout = null;

/**
 * Funzione per mostrare un messaggio sullo schermo del distributore
 */

function showDistMessage(text, isError = false, timeout = 4000) {
    const el = document.getElementById("dist-message");
    if (!el) return;
    el.textContent = text;

    // Imposta lo stile del messaggio (errore o info)
    el.dataset.type = isError ? "error" : "info";

    //elimino la classe che lo nasconde
    el.classList.remove("visually-hidden");


    if (messageTimeout) clearTimeout(messageTimeout);
    messageTimeout = setTimeout(() => {
        el.classList.add("visually-hidden");
        el.textContent = "";
    }, timeout);
}


 // Controlla periodicamente lo stato dell'utente connesso

async function pollConnectedUser() {
    try {
        const data = await fetchJSON(CONNECTED_URL);

        if (data && data.connected && data.user) {

            const serverUser = data.user;

            //creo una chiave unica per questo utente
            //mi serve per simulare la persistenza dati

            const storageKey = "credit_" + serverUser.username;

            //controllo se ho del credito in memoria
            const savedCredit = localStorage.getItem(storageKey);

            //se presente credito usiamo quello senno' quello del BACKEND fittizio

            let finalCredit = savedCredit !== null ? parseFloat(savedCredit) : Number(serverUser.credit);

            currentUser = {
                username: String(serverUser.username),
                fullName: String(serverUser.fullName),
                credit: finalCredit

            };

            //aggiorno UI
            document.getElementById("dist-username").textContent = `${currentUser.fullName} (${currentUser.username})`;
            const creditEl = document.getElementById("dist-credit");
            if (creditEl) {
                creditEl.dataset.value = String(currentUser.credit);
                creditEl.textContent = formatCurrency(currentUser.credit);
            }

            const form = document.getElementById("purchase-area");

            if (form) { form.style.display = ""; form.setAttribute("aria-hidden", "false"); }

        } else {
            // ... (Questa funzione non cambia)
            currentUser = null;
            document.getElementById("dist-username").textContent = "Nessun utente";
            const creditEl = document.getElementById("dist-credit");
            if (creditEl) { creditEl.dataset.value = "0"; creditEl.textContent = "0.00 €"; }

            const form = document.getElementById("purchase-area");
            if (form) { form.style.display = "none"; form.setAttribute("aria-hidden", "true"); }

            // Non mostrare l'errore se è solo la prima volta
            // showDistMessage("Nessun utente connesso.", true, 2500);
        }
    } catch (err) {
        console.error("Polling error:", err);
        showDistMessage("Errore connessione dati.", true, 3000);
    }
}

/**
 *  Gestisce la selezione delle bevande
 */
function initDrinkSelection() {
    const grid = document.getElementById("drink-grid");
    if (!grid) return;

    // Prende tutti i bottoni delle bevande dentro la griglia
    const buttons = grid.querySelectorAll(".drink-btn");

    buttons.forEach(button => {
        button.addEventListener("click", () => {
            // 1. Rimuovi la classe .selected da TUTTI i bottoni
            buttons.forEach(btn => btn.classList.remove("selected"));
            // 2. Aggiungi la classe .selected solo al bottone cliccato
            button.classList.add("selected");
        });
    });
}

/**
 * Gestisce il click sul pulsante "Eroga"
 */
function initPurchaseButtons() {
    const btn = document.getElementById("btn-purchase");
    if (!btn) return;

    btn.addEventListener("click", () => {
        if (!currentUser) {
            showDistMessage("Nessun utente connesso.", true, 2500);
            return;
        }

        // --- INIZIO MODIFICA ---
        // Cerca il bottone della bevanda SELEZIONATO
        const selectedBtn = document.querySelector("#drink-grid .drink-btn.selected");

        if (!selectedBtn) {
            showDistMessage("Seleziona prima una bevanda.", true, 3000);
            return;
        }

        // Prendi il prezzo dal 'data-price' del bottone selezionato
        const price = Number(selectedBtn.dataset.price) || 0;
        const drinkName = selectedBtn.textContent.split(" ")[0]; // Prende il nome (es. "Caffè")
        // --- FINE MODIFICA ---

        if (Number.isNaN(price) || price <= 0) {
            showDistMessage("Prezzo non valido.", true, 3000);
            return;
        }

        const creditCents = Math.round(currentUser.credit * 100);
        const priceCents = Math.round(price * 100);


        if (creditCents >= priceCents) {
            const newCreditCents = creditCents - priceCents;
            currentUser.credit = newCreditCents / 100;

            const storageKey = "credit_" + currentUser.username;
            //imposto il local storage
            localStorage.setItem(storageKey, currentUser.credit);

            const creditEl = document.getElementById("dist-credit");
            if (creditEl) {
                creditEl.dataset.value = String(currentUser.credit);
                creditEl.textContent = formatCurrency(currentUser.credit);
            }
            showDistMessage(`Erogazione ${drinkName} effettuata! Addebito: ${formatCurrency(price)}.`, false, 3500);

            // Deseleziona il bottone dopo l'acquisto
            selectedBtn.classList.remove("selected");

        } else {
            showDistMessage("Credito insufficiente.", true, 3000);
        }
    });
}

/**
 * Inizializzazione quando la pagina è pronta
 */
document.addEventListener("DOMContentLoaded", () => {
    pollConnectedUser().catch(e => console.error(e));

    initDrinkSelection();
    initPurchaseButtons();

    setInterval(pollConnectedUser, 5000);
});