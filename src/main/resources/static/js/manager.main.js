// manager.main.js - Gestione Dashboard

const DISTRIBUTORS_XML = "../data/esempio_stato.xml";
const MANUTENTORI_XML = "../data/manutentori.xml";

// Chiavi per il LocalStorage
const KEY_MANUTENTORI = "db_manutentori";
const KEY_DISTRIBUTORI = "db_distributori";

let manutentori = [];
let distributori = [];

// Variabile globale per tracciare quale distributore stiamo modificando col popup
let currentEditingId = null;

// ============================================================
// LOGICA MANUTENTORI
// ============================================================

async function initManutentori() {
    // 1. Controlla se abbiamo dati in memoria locale
    const saved = localStorage.getItem(KEY_MANUTENTORI);

    if (saved) {
        manutentori = JSON.parse(saved);
        renderManutentori();
    } else {
        // 2. Se non ci sono, carica da XML (prima volta)
        try {
            const xml = await fetchXML(MANUTENTORI_XML);
            const nodes = xml.getElementsByTagName("manutentore");
            manutentori = [];
            for (let n of nodes) {
                manutentori.push({
                    id: n.getAttribute("id"),
                    nome: n.querySelector("nome")?.textContent || '',
                    cognome: n.querySelector("cognome")?.textContent || '',
                    email: n.querySelector("email")?.textContent || '',
                    telefono: n.querySelector("telefono")?.textContent || ''
                });
            }
            // Salva nello storage per la prossima volta
            localStorage.setItem(KEY_MANUTENTORI, JSON.stringify(manutentori));
            renderManutentori();
        } catch (err) {
            console.error(err);
            showAlert("Errore caricamento XML Manutentori");
        }
    }
}

function renderManutentori() {
    const tbody = document.querySelector("#man-table tbody");
    if(!tbody) return;
    tbody.innerHTML = "";

    manutentori.forEach(m => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${m.id}</td>
            <td>${m.nome}</td>
            <td>${m.cognome}</td>
            <td>${m.email}</td>
            <td>${m.telefono}</td>
            <td><button class="man-del btn toggle-btn" data-id="${m.id}" style="background-color:var(--danger);">Elimina</button></td>
        `;
        tbody.appendChild(tr);
    });

    // Listener Elimina
    Array.from(document.getElementsByClassName("man-del")).forEach(btn => {
        btn.addEventListener("click", (e) => {
            const id = e.target.dataset.id;
            if(confirm("Vuoi davvero eliminare questo manutentore?")) {
                manutentori = manutentori.filter(m => m.id !== id);
                // Aggiorna Storage e UI
                localStorage.setItem(KEY_MANUTENTORI, JSON.stringify(manutentori));
                renderManutentori();
                showAlert(`Manutentore ${id} rimosso.`);
            }
        });
    });
}

// ============================================================
// LOGICA DISTRIBUTORI (Con Popup Stato)
// ============================================================

async function initDistributori() {
    const saved = localStorage.getItem(KEY_DISTRIBUTORI);

    if (saved) {
        distributori = JSON.parse(saved);
        renderAllDistributors();
    } else {
        try {
            const xml = await fetchXML(DISTRIBUTORS_XML);
            const nodes = xml.getElementsByTagName("distributore");
            distributori = [];
            for (let d of nodes) {
                distributori.push({
                    id: d.getAttribute("id"),
                    locazione: d.querySelector("locazione")?.textContent || '',
                    stato: d.querySelector("stato_operativo")?.textContent || 'NON ATTIVO'
                });
            }
            localStorage.setItem(KEY_DISTRIBUTORI, JSON.stringify(distributori));
            renderAllDistributors();
        } catch (err) {
            console.error(err);
            showAlert("Errore caricamento XML Distributori");
        }
    }
}

function renderAllDistributors(list = null) {
    const data = list || distributori;
    const container = document.getElementById("dist-list");
    if(!container) return;
    container.innerHTML = "";

    if (data.length === 0) {
        container.innerHTML = "<tr><td colspan='4' style='text-align:center;'>Nessun dato</td></tr>";
        return;
    }

    data.forEach(d => {
        // Colore dinamico in base allo stato
        let colorStyle = "color: #333;";
        if (d.stato === "Attivo") colorStyle = "color: #28a745; font-weight: bold;";
        else if (d.stato === "In manutenzione") colorStyle = "color: #d39e00; font-weight: bold;";
        else if (d.stato === "Disattivato") colorStyle = "color: #dc3545; font-weight: bold;";

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${d.id}</td>
            <td>${d.locazione}</td>
            <td style="${colorStyle}">${d.stato}</td>
            <td>
                <button data-id="${d.id}" class="btn-open-modal btn toggle-btn">Stato</button>
                <button data-id="${d.id}" class="dist-del btn toggle-btn" style="background-color:var(--danger);">Rimuovi</button>
            </td>
        `;
        container.appendChild(tr);
    });

    attachDistListeners();
}

function attachDistListeners() {
    // 1. APERTURA MODAL STATO
    Array.from(document.getElementsByClassName("btn-open-modal")).forEach(btn => {
        btn.addEventListener("click", (e) => {
            const id = e.target.dataset.id;
            currentEditingId = id; // Salviamo l'ID corrente

            // Imposta testo nel modal
            const labelId = document.getElementById("modal-dist-id");
            if(labelId) labelId.textContent = id;

            // Mostra il modal
            const modal = document.getElementById("modal-stato");
            if(modal) modal.classList.remove("visually-hidden");
        });
    });

    // 2. RIMOZIONE DISTRIBUTORE
    Array.from(document.getElementsByClassName("dist-del")).forEach(btn => {
        btn.addEventListener("click", (e) => {
            const id = e.target.dataset.id;
            if (confirm(`Sei sicuro di voler rimuovere il distributore ${id}?`)) {
                distributori = distributori.filter(d => d.id !== id);
                localStorage.setItem(KEY_DISTRIBUTORI, JSON.stringify(distributori));
                renderAllDistributors();
                showAlert(`Distributore ${id} rimosso.`);
            }
        });
    });
}

// ============================================================
// FUNZIONI GLOBALI PER IL MODAL (Chiamate dall'HTML onclick)
// ============================================================

function closeModal() {
    const modal = document.getElementById("modal-stato");
    if(modal) modal.classList.add("visually-hidden");
    currentEditingId = null;
}

function changeStatus(newStatus) {
    if (!currentEditingId) return;

    // Trova l'elemento nell'array
    const item = distributori.find(x => x.id === currentEditingId);
    if (item) {
        item.stato = newStatus;

        // Salva persistenza
        localStorage.setItem(KEY_DISTRIBUTORI, JSON.stringify(distributori));

        // Aggiorna UI e chiudi
        renderAllDistributors();
        closeModal();
        showAlert(`Stato aggiornato a ${newStatus}`);
    }
}

// ============================================================
// FUNZIONI DI RICERCA
// ============================================================

const btnSearch = document.getElementById("btn-search");
if(btnSearch){
    btnSearch.addEventListener("click", () => {
        const val = document.getElementById("search-id").value.trim();
        if(!val) return;
        const found = distributori.filter(d => d.id.includes(val));
        renderAllDistributors(found);
    });
}

const btnAll = document.getElementById("btn-all");
if(btnAll){
    btnAll.addEventListener("click", () => {
        document.getElementById("search-id").value = "";
        renderAllDistributors();
    });
}

// Avvio
document.addEventListener("DOMContentLoaded", () => {
    initManutentori();
    initDistributori();
});