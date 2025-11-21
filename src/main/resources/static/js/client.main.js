/**
 * Gestione Dashboard Cliente
 */

let loggedUser = null;

// Carica l'utente loggato dalla "sessione" (localStorage)
function loadSession() {
    const userStr = localStorage.getItem("loggedUser");
    if (!userStr) {
        alert("Accesso non autorizzato. Effettua il login.");
        window.location.href = "../login.html"; // Redirect se non loggato
        return;
    }
    loggedUser = JSON.parse(userStr);

    // Controlliamo se c'è un credito aggiornato salvato specificamente per questo utente
    // (Per gestire la persistenza della ricarica come fatto per il distributore)
    const savedCredit = localStorage.getItem("wallet_" + loggedUser.username);
    if (savedCredit !== null) {
        loggedUser.credit = parseFloat(savedCredit);
    }

    updateUI();
}

function updateUI() {
    document.getElementById("user-fullname").textContent = loggedUser.fullName;
    document.getElementById("user-credit").textContent = formatCurrency(loggedUser.credit);
}

// Gestione Connessione/Disconnessione
function initConnectionControls() {
    const btnConnect = document.getElementById("btn-connect");
    const btnDisconnect = document.getElementById("btn-disconnect");
    const inputId = document.getElementById("distributor-id");

    btnConnect.addEventListener("click", () => {
        const id = inputId.value.trim();
        if (id) {
            alert(`Connesso al distributore ${id}`);
            // In futuro qui faremo una chiamata fetch POST al backend
        } else {
            alert("Inserisci un ID distributore valido.");
        }
    });

    btnDisconnect.addEventListener("click", () => {
        alert("Disconnesso dal distributore.");
        inputId.value = ""; // Pulisce il campo
    });
}

// Gestione Ricarica
function initRechargeControl() {
    const btnRecharge = document.getElementById("btn-recharge");
    const inputAmount = document.getElementById("recharge-amount");

    btnRecharge.addEventListener("click", () => {
        const amount = parseFloat(inputAmount.value);
        if (amount > 0) {
            // Aggiorna modello dati
            loggedUser.credit += amount;

            // Salva persistenza
            localStorage.setItem("wallet_" + loggedUser.username, loggedUser.credit);
            // Aggiorniamo anche l'oggetto sessione principale per coerenza
            localStorage.setItem("loggedUser", JSON.stringify(loggedUser));

            updateUI();
            alert(`Ricarica di ${formatCurrency(amount)} effettuata con successo!`);
            inputAmount.value = ""; // Reset campo
        } else {
            alert("Inserisci un importo valido.");
        }
    });

    // Logout manuale
    document.getElementById("btn-logout").addEventListener("click", () => {
        localStorage.removeItem("loggedUser");
        window.location.href = "../login.html";
    });
}

document.addEventListener("DOMContentLoaded", () => {
    loadSession();
    initConnectionControls();
    initRechargeControl();
});