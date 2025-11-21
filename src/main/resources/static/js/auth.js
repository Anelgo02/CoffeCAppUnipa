const USERS_URL = "../data/users.json";

async function handleLogin(event) {
    event.preventDefault(); // Ferma il submit standard del form HTML

    const emailInput = document.getElementById("email").value.trim();
    const passwordInput = document.getElementById("password").value.trim();

    // Validazione HTML5 check (extra sicurezza)
    if (!event.target.checkValidity()) {
        event.target.reportValidity();
        return;
    }

    try {
        const users = await fetchJSON(USERS_URL); // Usa la tua apiHelpers.js

        // Cerchiamo l'utente che corrisponde a email E password
        const user = users.find(u => u.email === emailInput && u.password === passwordInput);

        if (user) {
            // LOGIN RIUSCITO

            // 1. Salviamo la sessione (Simulazione Cookie/Token)
            // Salviamo tutto l'oggetto utente per usarlo nelle altre pagine
            localStorage.setItem("loggedUser", JSON.stringify(user));

            // 2. Routing in base al Ruolo
            switch (user.role) {
                case "CLIENTE":
                    window.location.href = "../cliente/index.html";
                    break;
                case "MANUTENTORE":
                    window.location.href = "../manutenzione/index.html"; // o la tua pagina manutentore
                    break;
                case "GESTORE":
                    window.location.href = "../gestore/index.html"; // o la tua pagina gestore
                    break;
                default:
                    alert("Ruolo non riconosciuto: " + user.role);
            }
        } else {
            // LOGIN FALLITO
            alert("Credenziali non valide (Usa le credenziali nel file users.json)");
        }

    } catch (err) {
        console.error("Errore login:", err);
        alert("Errore di sistema durante il login.");
    }
}

// Collega la funzione al form se esiste nella pagina
document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("login-form");
    if (loginForm) {
        loginForm.addEventListener("submit", handleLogin);
    }
});