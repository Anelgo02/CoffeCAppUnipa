// js/auth.register.js

const USERS_URL = "../data/users.json";
const KEY_USERS = "db_users"; // Chiave dove salviamo tutti gli utenti

async function handleRegister(event) {
    event.preventDefault(); // Blocca il form classico

    const nome = document.getElementById("nome").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value.trim();

    if (!nome || !email || !password) {
        alert("Compila tutti i campi.");
        return;
    }

    try {
        // 1. Recupera la lista utenti esistente (o carica dal JSON se è la prima volta)
        let users = [];
        const savedUsers = localStorage.getItem(KEY_USERS);

        if (savedUsers) {
            users = JSON.parse(savedUsers);
        } else {
            // Se non abbiamo mai salvato utenti, leggiamo quelli iniziali dal file
            const res = await fetchJSON(USERS_URL);
            users = res;
        }

        // 2. Controllo duplicati
        const exists = users.find(u => u.email === email);
        if (exists) {
            alert("Questa email è già registrata.");
            return;
        }

        // 3. Creazione Nuovo Utente Cliente
        const newUser = {
            username: email.split('@')[0], // Genera username dalla mail
            email: email,
            password: password,
            role: "CLIENTE",
            fullName: nome,
            credit: 0.00 // Credito iniziale zero
        };

        // 4. Salva nel "Database"
        users.push(newUser);
        localStorage.setItem(KEY_USERS, JSON.stringify(users));

        // 5. AUTO-LOGIN: Salviamo l'utente in sessione
        localStorage.setItem("loggedUser", JSON.stringify(newUser));

        // Inizializziamo anche il credito condiviso (per il distributore)
        localStorage.setItem("credit_" + newUser.username, 0.00);

        alert("Registrazione avvenuta con successo! Benvenuto " + nome);

        // 6. Redirect alla dashboard (siamo dentro /cliente/, quindi main.html è lì vicino)
        window.location.href = "index.html";

    } catch (err) {
        console.error("Errore registrazione:", err);
        alert("Si è verificato un errore tecnico.");
    }
}

// Avvio
document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("form-register");
    if (form) {
        form.addEventListener("submit", handleRegister);
    }
});