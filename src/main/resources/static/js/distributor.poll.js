// /js/distributor.poll.js

// --- API ENDPOINTS ---
function getDistributorCode() {
    const params = new URLSearchParams(window.location.search);
    return (params.get("code") || "").trim();
}

let currentUser = null;
let messageTimeout = null;
const MONITOR_BASE_URL = "http://localhost:8081/CoffeeMonitor_war_exploded";



/**
 * Mostra messaggio sullo schermo distributore
 */
function showDistMessage(text, isError = false, timeout = 4000) {
    const el = document.getElementById("dist-message");
    if (!el) return;

    el.textContent = text;
    el.dataset.type = isError ? "error" : "info";
    el.classList.remove("visually-hidden");

    if (messageTimeout) clearTimeout(messageTimeout);
    messageTimeout = setTimeout(() => {
        el.classList.add("visually-hidden");
        el.textContent = "";
    }, timeout);
}

/**
 * Aggiorna UI info utente + credito + mostra/nasconde form acquisto
 * + mostra/nasconde bottone "Scollega"
 */
function renderConnectedState(connected, user) {
    const usernameEl = document.getElementById("dist-username");
    const creditEl = document.getElementById("dist-credit");
    const form = document.getElementById("purchase-area");

    const btnDisconnect = document.getElementById("btn-disconnect-from-dist");

    if (!connected) {
        currentUser = null;

        if (usernameEl) usernameEl.textContent = "Nessun utente";
        if (creditEl) {
            creditEl.dataset.value = "0";
            creditEl.textContent = "0.00 €";
        }
        if (form) {
            form.style.display = "none";
            form.setAttribute("aria-hidden", "true");
        }
        if (btnDisconnect) btnDisconnect.style.display = "none";
        return;
    }

    currentUser = user;

    if (usernameEl) usernameEl.textContent = `${user.username}`;
    if (creditEl) {
        creditEl.dataset.value = String(user.credit ?? 0);
        creditEl.textContent = formatCurrency(user.credit ?? 0);
    }
    if (form) {
        form.style.display = "";
        form.setAttribute("aria-hidden", "false");
    }
    if (btnDisconnect) btnDisconnect.style.display = "";


}

/**
 * POLL: legge dal backend l'eventuale utente connesso
 * GET /api/distributor/poll?code=...
 */

async function pollConnectedUser() {
    const code = getDistributorCode();

    // --- MODIFICA 1: Controllo Codice mancante ---
    if (!code) {
        // Mostra il messaggio
        showDistMessage("Codice mancante. Ritorno alla home...", true, 3000);
        renderConnectedState(false);

        // Aspetta 3 secondi per far leggere il messaggio, poi reindirizza
        setTimeout(() => {
            window.location.href = "/cliente/index.html"; // Cambia con il percorso della tua home
        }, 3000);
        return;
    }

    try {
        const data = await apiGetJSON(`/api/distributor/poll?code=${encodeURIComponent(code)}`);

        if (!data || !data.ok) throw new Error("Risposta non valida");

        // --- MODIFICA 2: Utente non connesso ---
        if (!data.connected) {
            renderConnectedState(false);

            // Opzionale: Mostra un avviso visivo che ci si sta scollegando
            showDistMessage("Nessun utente connesso. Chiusura...", true, 2000);

            // Reindirizza dopo 2 secondi
            setTimeout(() => {
                window.location.href = "/cliente/index.html";
            }, 2000);
            return;
        }

        // --- CASO 3: Utente Connesso (Resta qui) ---
        // dal backend: username, credit
        renderConnectedState(true, {
            username: data.username || "-",
            credit: Number(data.credit ?? 0)
        });

    } catch (err) {
        console.error("Polling error:", err);
        showDistMessage("Errore connessione dati.", true, 3000);
        renderConnectedState(false);
        // Reindirizza dopo 2 secondi
        setTimeout(() => {
            window.location.href = "/cliente/index.html";
        }, 2000);
    }
}

/**
 * Carica la lista bevande da DB e popola la tua griglia #drink-grid
 * GET /api/distributor/beverages
 */
async function loadBeveragesIntoGrid() {
    const grid = document.getElementById("drink-grid");
    if (!grid) return;

    try {
        const data = await apiGetJSON("/api/distributor/beverages");
        if (!data || !data.ok) throw new Error("Risposta non valida");

        grid.innerHTML = "";

        data.items.forEach((b) => {
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "drink-btn";
            btn.dataset.beverageId = String(b.id);
            btn.dataset.price = String(b.price);

            btn.innerHTML = `${escapeHtml(b.name)} <small>${formatCurrency(b.price)}</small>`;

            btn.addEventListener("click", () => {
                grid.querySelectorAll(".drink-btn").forEach(x => x.classList.remove("selected"));
                btn.classList.add("selected");
            });

            grid.appendChild(btn);
        });

    } catch (err) {
        console.error(err);
        showDistMessage("Errore caricamento bevande.", true, 3500);
    }
}

/**
 * Eroga bevanda: POST /api/distributor/purchase
 * Parametri: code, beverageId, sugarQty
 */
async function doPurchase() {
    const code = getDistributorCode();
    if (!code) {
        showDistMessage("Manca ?code=... nell'URL", true, 4000);
        return;
    }

    if (!currentUser) {
        showDistMessage("Nessun utente connesso.", true, 2500);
        return;
    }

    const selectedBtn = document.querySelector("#drink-grid .drink-btn.selected");
    if (!selectedBtn) {
        showDistMessage("Seleziona prima una bevanda.", true, 3000);
        return;
    }

    const beverageId = selectedBtn.dataset.beverageId;
    const sugarQty = document.getElementById("sugar-qty")?.value ?? "0";

    try {
        const res = await apiPostForm("/api/distributor/purchase", {
            code,
            beverageId,
            sugarQty
        });

        if (res && res.ok) {
            const creditEl = document.getElementById("dist-credit");
            if (creditEl && typeof res.credit !== "undefined") {
                creditEl.dataset.value = String(res.credit);
                creditEl.textContent = formatCurrency(res.credit);
            }
            showDistMessage("Erogazione effettuata!", false, 3000);

            selectedBtn.classList.remove("selected");
            pollConnectedUser();
            return;
        }

        showDistMessage("Erogazione inviata.", false, 2500);
        pollConnectedUser();

    } catch (err) {
        console.error(err);

        const m = (err.message || "").toLowerCase();
        if (m.includes("credito insufficiente")) {
            showDistMessage("Credito insufficiente.", true, 3000);
            return;
        }
        if (m.includes("nessun cliente connesso")) {
            showDistMessage("Nessun utente connesso.", true, 3000);
            pollConnectedUser();
            return;
        }

        showDistMessage("Errore erogazione: " + err.message, true, 3500);
        pollConnectedUser();
    }
}

/**
 * DISCONNECT dal distributore: in realtà è il customer che si disconnette.
 * POST /api/customer/disconnect
 */
async function disconnectFromDistributor() {
    try {
        const res = await apiPostForm("/api/customer/disconnect", {});
        if (res && res.ok) {
            showDistMessage("Scollegato.", false, 2500);
        } else {
            showDistMessage("Richiesta di scollegamento inviata.", false, 2500);
        }
    } catch (err) {
        console.error(err);
        showDistMessage("Errore scollegamento: " + err.message, true, 3500);
    } finally {
        pollConnectedUser();
    }
}

/**
 * HEARTBEAT ogni 60s verso la mini-app di monitoraggio (che faremo dopo).
 * Per ora: se fallisce, ignoriamo.
 */
async function sendHeartbeat() {
    const code = getDistributorCode();
    if (!code) return;

    try {
        await fetch(`${MONITOR_BASE_URL}/api/monitor/heartbeat`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: new URLSearchParams({ code }),
            cache: "no-cache"
        });
    } catch (_) {
        // ignore
    }
}

function escapeHtml(s) {
    return (s ?? "").toString()
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}

// INIT
document.addEventListener("DOMContentLoaded", () => {
    loadBeveragesIntoGrid();
    pollConnectedUser().catch(e => console.error(e));

    const btnPurchase = document.getElementById("btn-purchase");
    if (btnPurchase) {
        btnPurchase.addEventListener("click", () => {
            doPurchase().catch(e => console.error(e));
        });
    }

    const distHeader = document.querySelector("#dist-header");
    const code = getDistributorCode();
    if(distHeader) {
        distHeader.textContent += ` ${code}`;
    }



    const btnDisconnect = document.getElementById("btn-disconnect-from-dist");
    if (btnDisconnect) {
        btnDisconnect.addEventListener("click", () => {
            disconnectFromDistributor().catch(e => console.error(e));
        });
    }

    setInterval(pollConnectedUser, 5000);
    setInterval(sendHeartbeat, 60000);
});
