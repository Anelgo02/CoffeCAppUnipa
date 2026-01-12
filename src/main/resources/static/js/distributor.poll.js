// /js/distributor.poll.js

// --- CONFIG ---
const MONITOR_BASE_URL = "http://localhost:8081/CoffeeMonitor_war_exploded";
let currentUser = null;
let messageTimeout = null;

function getDistributorCode() {
    const params = new URLSearchParams(window.location.search);
    return (params.get("code") || "").trim();
}

/**
 * Gestione UI Messaggi
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
 * LOGICA CORE: Cambio Stato (Standby <-> Attivo)
 */
function renderConnectedState(connected, user) {
    const standbyScreen = document.getElementById("standby-screen");
    const activeScreen = document.getElementById("distributor-screen");

    const usernameEl = document.getElementById("dist-username");
    const creditEl = document.getElementById("dist-credit");
    const form = document.getElementById("purchase-area");

    if (!connected) {
        // --- STATO STANDBY ---
        currentUser = null;

        if (standbyScreen) standbyScreen.style.display = "block";
        if (activeScreen) activeScreen.style.display = "none";

        if (form) {
            form.style.display = "none";
            form.setAttribute("aria-hidden", "true");
        }
        return;
    }

    // --- STATO ATTIVO ---
    currentUser = user;

    if (standbyScreen) standbyScreen.style.display = "none";
    if (activeScreen) activeScreen.style.display = "block";

    if (usernameEl) usernameEl.textContent = `${user.username}`;
    if (creditEl) {
        creditEl.dataset.value = String(user.credit ?? 0);
        creditEl.textContent = formatCurrency(user.credit ?? 0);
    }
    if (form) {
        form.style.display = "block";
        form.setAttribute("aria-hidden", "false");
    }
}

/**
 * POLL: Chiede al server se c'è qualcuno
 */
async function pollConnectedUser() {
    const code = getDistributorCode();

    // Se manca il codice, lo script guardiano nell'HTML dovrebbe aver già fatto redirect.
    // Ma per sicurezza:
    if (!code) return;

    try {
        const data = await apiGetJSON(`/api/distributor/poll?code=${encodeURIComponent(code)}`);

        if (!data || !data.ok) throw new Error("Risposta server non valida");

        if (!data.connected) {
            // Nessuno connesso -> Mostra Standby
            renderConnectedState(false);
        } else {
            // Qualcuno connesso -> Mostra Interfaccia Vendita
            renderConnectedState(true, {
                username: data.username || "Cliente",
                credit: Number(data.credit ?? 0)
            });
        }

    } catch (err) {
        console.error("Polling error:", err);
        // In caso di errore rete, torniamo prudentemente in standby
        renderConnectedState(false);
    }
}

/**
 * Caricamento Listino Bevande
 */
async function loadBeveragesIntoGrid() {
    const grid = document.getElementById("drink-grid");
    if (!grid) return;

    try {
        const data = await apiGetJSON("/api/distributor/beverages");
        if (!data || !data.ok) throw new Error("Risposta listino non valida");

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
        showDistMessage("Errore caricamento listino.", true, 5000);
    }
}

/**
 * Acquisto
 */
async function doPurchase() {
    const code = getDistributorCode();
    if (!code || !currentUser) {
        showDistMessage("Errore: sessione non valida.", true);
        return;
    }

    const selectedBtn = document.querySelector("#drink-grid .drink-btn.selected");
    if (!selectedBtn) {
        showDistMessage("Seleziona una bevanda.", true, 3000);
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
                creditEl.textContent = formatCurrency(res.credit);
            }
            showDistMessage("Erogazione in corso... Prendi il tuo caffè!", false, 4000);
            selectedBtn.classList.remove("selected");
            return;
        }

        // Se arriviamo qui, c'è stato un problema ma non un'eccezione
        showDistMessage("Errore generico erogazione.", true, 3000);

    } catch (err) {
        // Gestione errori specifici dal backend
        const m = (err.message || "").toLowerCase();

        if (m.includes("credito")) {
            showDistMessage("Credito insufficiente!", true, 3000);
        } else if (m.includes("scorte")) {
            showDistMessage("Prodotto esaurito (Scorte insufficienti).", true, 3000);
        } else if (m.includes("nessun cliente")) {
            showDistMessage("Sessione scaduta.", true, 2000);
            renderConnectedState(false);
        } else {
            showDistMessage("Errore sistema: " + err.message, true, 3000);
        }
    }
}

/**
 * Heartbeat verso Monitoraggio
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
    } catch (_) { /* ignore failure */ }
}

function escapeHtml(s) {
    return (s ?? "").toString().replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

// INIT
document.addEventListener("DOMContentLoaded", () => {
    loadBeveragesIntoGrid();
    pollConnectedUser(); // Primo controllo immediato

    const btnPurchase = document.getElementById("btn-purchase");
    if (btnPurchase) {
        btnPurchase.addEventListener("click", doPurchase);
    }

    // Polling ogni 3 secondi (più reattivo)
    setInterval(pollConnectedUser, 3000);
    // Heartbeat ogni 60 secondi
    setInterval(sendHeartbeat, 60000);
});