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

    if (btnLogout) {
        btnLogout.addEventListener("click", async () => {
            try {
                await apiPostForm("/auth/logout", {});
            } finally {
                window.location.href = "/login.html";
            }
        });

    }

    async function loadCustomerMe() {
        try {
            const data = await apiGetJSON("/api/customer/me");
            if (!data.ok) throw new Error("Risposta non valida");

            elName.textContent = data.username || "Cliente";
            elCredit.textContent = formatCurrency(data.credit ?? 0);
        } catch (err) {
            console.error(err);
            elName.textContent = "Cliente";
            elCredit.textContent = "—";
        }
    }

    async function refreshConnectionStatus() {
        try {
            const data = await apiGetJSON("/api/customer/current-connection");

            if (data && data.connected) {
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
            showAlert("Errore: " + err.message);
        }
    }

    if (btnConnect) {
        btnConnect.addEventListener("click", async () => {
            const code = (distInput.value || "").trim();
            if (!code) {
                showAlert("Inserisci l'ID/Codice del distributore (es. D-001).");
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
    }

    if (btnDisconnect) {
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
    }

    if (rechargeForm) {
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
    }

    // INIT
    loadCustomerMe();
    refreshConnectionStatus();
    // Polling ogni 3 secondi per aggiornare il credito in tempo reale
    setInterval(loadCustomerMe, 3000);
});
