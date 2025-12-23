/**
 * Dashboard Cliente - versione DB + sessione server
 * Richiede: apiHelpers.js (apiGetJSON, apiPostForm, formatCurrency, showAlert)
 */

document.addEventListener("DOMContentLoaded", () => {
    const btnLogout = document.getElementById("btn-logout");

    const elName = document.getElementById("user-fullname");
    const elCredit = document.getElementById("user-credit");

    const btnConnect = document.getElementById("btn-connect");
    const btnDisconnect = document.getElementById("btn-disconnect");
    const distInput = document.getElementById("distributor-id");

    const rechargeForm = document.querySelector("#ricarica form");
    const rechargeInput = document.getElementById("recharge-amount");

    btnLogout.addEventListener("click", () => {
        window.location.href = "/route/logout";
    });

    async function loadCustomerMe() {
        try {
            const data = await apiGetJSON("/api/customer/me");
            if (!data.ok) throw new Error("Risposta non valida");

            elName.textContent = data.username || "Cliente";
            elCredit.textContent = formatCurrency(data.credit);

        } catch (err) {
            console.error(err);
            elName.textContent = "Cliente";
            elCredit.textContent = "—";
        }
    }

    async function refreshConnectionStatus() {
        try {
            const data = await apiGetJSON("/api/customer/current-connection");

            if (data.connected) {
                btnConnect.disabled = true;
                btnDisconnect.disabled = false;
                distInput.disabled = true;
            } else {
                btnConnect.disabled = false;
                btnDisconnect.disabled = true;
                distInput.disabled = false;
            }
        } catch (err) {
            console.error(err);
            window.location.href = "/login.html?err=session";
        }
    }

    btnConnect.addEventListener("click", async () => {
        const code = (distInput.value || "").trim();
        if (!code) {
            showAlert("Inserisci l'ID/Codice del distributore (es. UNIPA-001).");
            return;
        }

        try {
            await apiPostForm("/api/customer/connect", { code });
            showAlert("Connesso al distributore!");
            await refreshConnectionStatus();
            await loadCustomerMe();
        } catch (err) {
            console.error(err);
            showAlert("Errore connessione: " + err.message);
        }
    });

    btnDisconnect.addEventListener("click", async () => {
        try {
            await apiPostForm("/api/customer/disconnect", {});
            showAlert("Disconnesso.");
            distInput.value = "";
            await refreshConnectionStatus();
            await loadCustomerMe();
        } catch (err) {
            console.error(err);
            showAlert("Errore disconnessione: " + err.message);
        }
    });

    rechargeForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const amount = parseFloat(rechargeInput.value);
        if (isNaN(amount) || amount <= 0) {
            showAlert("Inserisci un importo valido.");
            return;
        }

        try {
            const data = await apiPostForm("/api/customer/topup", { amount: amount.toString() });

            if (data && data.ok && typeof data.credit !== "undefined") {
                elCredit.textContent = formatCurrency(data.credit);
                showAlert("Ricarica effettuata!");
                rechargeInput.value = "";
            } else {
                showAlert("Ricarica effettuata, aggiorna la pagina.");
            }

            await loadCustomerMe();

        } catch (err) {
            console.error(err);
            showAlert("Errore ricarica: " + err.message);
        }
    });

    loadCustomerMe();
    refreshConnectionStatus();
});
