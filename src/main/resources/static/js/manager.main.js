const DISTRIBUTORS_XML = "../data/esempio_stato.xml";
const MANUTENTORI_XML = "../data/manutentori.xml";

let manutentori = []; // array di object
let distributoriXmlDoc = null;

async function loadManutentori() {
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
        renderManutentori();
    } catch (err) {
        console.error(err);
        showAlert("Impossibile caricare manutentori.");
    }
}
function renderManutentori() {
    const tbody = document.querySelector("#man-table tbody");
    tbody.innerHTML = "";
    manutentori.forEach(m => {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${m.id}</td><td>${m.nome}</td><td>${m.cognome}</td><td>${m.email}</td><td>${m.telefono}</td>
      <td><button class="man-del btn toggle-btn" data-id="${m.id}">Elimina</button></td>`;
        tbody.appendChild(tr);
    });
    Array.from(document.getElementsByClassName("man-del")).forEach(btn => {
        btn.addEventListener("click", (e) => {
            const id = e.target.dataset.id;
            manutentori = manutentori.filter(m => m.id !== id);
            renderManutentori();
            showAlert(`Manutentore ${id} rimosso (simulato).`);
        });
    });
}

document.getElementById("m-add").addEventListener("click", () => {
    const id = document.getElementById("m-id").value.trim();
    if (!id) { showAlert("ID richiesto"); return; }
    const newM = {
        id,
        nome: document.getElementById("m-nome").value.trim(),
        cognome: document.getElementById("m-cognome").value.trim(),
        email: document.getElementById("m-email").value.trim(),
        telefono: document.getElementById("m-telefono").value.trim()
    };
    manutentori.push(newM);
    renderManutentori();
    showAlert(`Aggiunto manutentore ${id} (simulato).`);
});

// Distributori / ricerca
async function loadDistributorsXml() {
    try {
        distributoriXmlDoc = await fetchXML(DISTRIBUTORS_XML);
    } catch (err) {
        console.error(err);
        showAlert("Impossibile caricare file distributori.");
    }
}

function renderAllDistributors() {
    const nodes = distributoriXmlDoc.getElementsByTagName("distributore");
    const container = document.getElementById("dist-list");
    container.innerHTML = "";
    Array.from(nodes).forEach(d => {
        const id = d.getAttribute("id");
        const loc = d.querySelector("locazione")?.textContent || '';
        const stato = d.querySelector("stato_operativo")?.textContent || '';
        const tr = document.createElement("tr");
        tr.innerHTML = `<td>${id}</td><td>${loc}</td><td id="state-${id}">${stato}</td>
        <td><button data-id="${id}" class="toggle-active btn toggle-btn">Attiva/Disattiva</button></td>`;
        container.appendChild(tr);
    });
    Array.from(document.getElementsByClassName("toggle-active")).forEach(btn => {
        btn.addEventListener("click", (e) => {
            const id = e.target.dataset.id;
            const elState = document.getElementById(`state-${id}`);
            // Simulazione toggle
            elState.textContent = (elState.textContent === "attivo") ? "disattivato" : "attivo";
            showAlert(`Stato distributore ${id} modificato (simulato).`);
        });
    });
}

document.getElementById("btn-search").addEventListener("click", () => {
    const id = document.getElementById("search-id").value.trim();
    if (!distributoriXmlDoc) { showAlert("Carica prima i distributori"); return; }
    const nodes = distributoriXmlDoc.getElementsByTagName("distributore");
    let found = null;
    for (let d of nodes) if (d.getAttribute("id") === id) { found = d; break; }
    const container = document.getElementById("dist-list");
    container.innerHTML = "";
    if (!found) { container.innerHTML = `<p>Distributore ${id} non trovato</p>`; return; }
    const idn = found.getAttribute("id");
    const loc = found.querySelector("locazione")?.textContent || '';
    const stato = found.querySelector("stato_operativo")?.textContent || '';
    container.innerHTML = `<div><strong>${idn}</strong> - ${loc} - Stato: <span id="state-${idn}">${stato}</span></div>`;
});

document.getElementById("btn-all").addEventListener("click", () => {
    if (!distributoriXmlDoc) { showAlert("Carica prima i distributori"); return; }
    renderAllDistributors();
});

// init
(async function() {
    await loadManutentori();
    await loadDistributorsXml();
    renderAllDistributors();
})();