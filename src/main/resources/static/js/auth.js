const USERS_URL = "../data/users.json";
const KEY_USERS = "db_users";

async function handleLogin(event) {
    event.preventDefault();

    const emailInput = document.getElementById("email").value.trim();
    const passwordInput = document.getElementById("password").value.trim();

    try {
        let users = [];

        // 1. PRIMA controlliamo se abbiamo un database aggiornato in memoria
        const savedUsers = localStorage.getItem(KEY_USERS);

        if (savedUsers) {
            users = JSON.parse(savedUsers);
        } else {
            // 2. Se non c'è memoria, carichiamo il file originale
            users = await fetchJSON(USERS_URL);

            // Salviamo questi utenti in memoria per la prossima volta
            localStorage.setItem(KEY_USERS, JSON.stringify(users));
        }

        // 3. Cerchiamo l'utente
        const user = users.find(u => u.email === emailInput && u.password === passwordInput);

        if (user) {
            // Login OK
            localStorage.setItem("loggedUser", JSON.stringify(user));

            // Redirect in base al ruolo
            switch (user.role) {
                case "CLIENTE":
                    window.location.href = "cliente/index.html";
                    break;
                case "MANUTENTORE":
                    window.location.href = "manutenzione/index.html";
                    break;
                case "GESTORE":
                    window.location.href = "gestore/index.html";
                    break;
                default:
                    alert("Ruolo non riconosciuto.");
            }
        } else {
            alert("Credenziali non valide.");
        }

    } catch (err) {
        console.error("Errore login:", err);
        alert("Errore di sistema. Controlla la console.");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("login-form");
    if (loginForm) {
        loginForm.addEventListener("submit", handleLogin);
    }
});