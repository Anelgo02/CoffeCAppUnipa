// manager.add.js - Gestione Form di Aggiunta

const KEY_MANUTENTORI = "db_manutentori";
const KEY_DISTRIBUTORI = "db_distributori";

// --- GESTIONE AGGIUNTA MANUTENTORE ---
const btnSaveMan = document.getElementById("btn-save-man");
if (btnSaveMan) {
    btnSaveMan.addEventListener("click", () => {
        const id = document.getElementById("m-id").value.trim();
        if (!id) { alert("ID richiesto"); return; }

        const newItem = {
            id: id,
            nome: document.getElementById("m-nome").value.trim(),
            cognome: document.getElementById("m-cognome").value.trim(),
            email: document.getElementById("m-email").value.trim(),
            telefono: document.getElementById("m-telefono").value.trim()
        };

        // 1. Carica array esistente o crealo vuoto
        let list = JSON.parse(localStorage.getItem(KEY_MANUTENTORI)) || [];

        // 2. Aggiungi
        list.push(newItem);

        // 3. Salva
        localStorage.setItem(KEY_MANUTENTORI, JSON.stringify(list));

        alert("Manutentore aggiunto con successo!");
        // 4. Torna alla home
        window.location.href = "index.html";
    });
}

// --- GESTIONE AGGIUNTA DISTRIBUTORE ---
const btnSaveDist = document.getElementById("btn-save-dist");
if (btnSaveDist) {
    btnSaveDist.addEventListener("click", () => {
        const id = document.getElementById("d-id").value.trim();
        if (!id) { alert("ID richiesto"); return; }

        const newItem = {
            id: id,
            locazione: document.getElementById("d-loc").value.trim(),
            stato: document.getElementById("d-stato").value
        };

        // 1. Carica array esistente
        let list = JSON.parse(localStorage.getItem(KEY_DISTRIBUTORI)) || [];

        // 2. Aggiungi
        list.push(newItem);

        // 3. Salva
        localStorage.setItem(KEY_DISTRIBUTORI, JSON.stringify(list));

        alert("Distributore aggiunto con successo!");
        // 4. Torna alla home
        window.location.href = "index.html";
    });
}