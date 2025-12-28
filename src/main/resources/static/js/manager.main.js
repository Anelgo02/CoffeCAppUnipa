document.addEventListener("DOMContentLoaded", () => {
    const btnLogout = document.getElementById("btn-logout");

    // --- DISTRIBUTORI ---
    const distTbody = document.getElementById("dist-list");
    const searchInput = document.getElementById("search-id");
    const btnSearch = document.getElementById("btn-search");
    const btnAll = document.getElementById("btn-all");

    // Modal stato
    const modal = document.getElementById("modal-stato");
    const modalDistId = document.getElementById("modal-dist-id");
    let currentDistIdForModal = null;

    // --- MANUTENTORI ---
    const manTable = document.querySelector("#man-table tbody");

    // Cache distributori (per filtro frontend)
    let allDistributors = [];

    // Logout (opzionale)
    if (btnLogout) {
        btnLogout.addEventListener("click", (e) => {
        });
    }

    // ------------------ HELPERS ------------------

    function norm(s) {
        return (s ?? "")
            .toString()
            .trim()
            .toUpperCase()
            .replace(/\s+/g, " ");
    }

    function normalizeStatusForSearch(stato) {
        const v = norm(stato);
        if (v === "ACTIVE" || v === "ATTIVO") return "ATTIVO";
        if (v === "MAINTENANCE" || v === "MANUTENZIONE" || v === "IN MANUTENZIONE") return "MANUTENZIONE";
        if (v === "FAULT" || v === "DISATTIVO" || v === "DISABLED") return "DISATTIVO";
        return v;
    }

    function mapItalianQueryToDbStatusToken(qUpper) {
        if (qUpper === "ATTIVO") return "ACTIVE";
        if (qUpper === "MANUTENZIONE" || qUpper === "IN MANUTENZIONE") return "MAINTENANCE";
        if (qUpper === "DISATTIVO") return "FAULT";
        return "";
    }

    function renderDistributors(list) {
        distTbody.innerHTML = "";

        list.forEach((d) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
        <td>${escapeHtml(d.id)}</td>
        <td>${escapeHtml(d.luogo)}</td>
        <td>${escapeHtml(d.stato)}</td>
        <td style="display:flex; gap:8px; flex-wrap:wrap;">
          <button class="btn" data-status="${escapeHtml(d.id)}">Cambia stato</button>
          <button class="btn btn-secondario" data-del-dist="${escapeHtml(d.id)}">Elimina</button>
        </td>
      `;
            distTbody.appendChild(tr);
        });

        // bind status modal
        distTbody.querySelectorAll("[data-status]").forEach((btn) => {
            btn.addEventListener("click", () => {
                currentDistIdForModal = btn.getAttribute("data-status");
                window.openModal(currentDistIdForModal);
            });
        });

        // bind delete
        distTbody.querySelectorAll("[data-del-dist]").forEach((btn) => {
            btn.addEventListener("click", async () => {
                const id = btn.getAttribute("data-del-dist");
                if (!confirm(`Eliminare distributore ${id}?`)) return;

                try {
                    await apiPostForm("/api/manager/distributors/delete", { id });
                    showAlert("Distributore eliminato.");
                    await loadDistributors();
                    applyFilter();
                } catch (err) {
                    console.error(err);
                    showAlert("Errore eliminazione: " + err.message);
                }
            });
        });
    }

    function applyFilter() {
        const q = norm(searchInput.value);

        if (!q) {
            renderDistributors(allDistributors);
            return;
        }

        const dbToken = mapItalianQueryToDbStatusToken(q);

        const filtered = allDistributors.filter((d) => {
            const id = norm(d.id);
            const luogo = norm(d.luogo);

            const statoRaw = norm(d.stato);
            const statoUi = normalizeStatusForSearch(d.stato);

            return (
                id.includes(q) ||
                luogo.includes(q) ||
                statoUi.includes(q) ||
                statoRaw.includes(q) ||
                (dbToken && statoRaw.includes(dbToken))
            );
        });

        renderDistributors(filtered);
    }

    // ------------------ LOADERS ------------------

    async function loadMaintainers() {
        manTable.innerHTML = "";
        try {
            const data = await apiGetJSON("/api/manager/maintainers/list");
            if (!data.ok) throw new Error("Risposta non valida");

            data.items.forEach((m) => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
          <td>${escapeHtml(m.id)}</td>
          <td>${escapeHtml(m.nome)}</td>
          <td>${escapeHtml(m.cognome)}</td>
          <td>${escapeHtml(m.email)}</td>
          <td>${escapeHtml(m.telefono)}</td>
          <td>
            <button class="btn btn-secondario" data-del-man="${escapeHtml(m.id)}">Elimina</button>
          </td>
        `;
                manTable.appendChild(tr);
            });

            manTable.querySelectorAll("[data-del-man]").forEach((btn) => {
                btn.addEventListener("click", async () => {
                    const id = btn.getAttribute("data-del-man");
                    if (!confirm(`Eliminare manutentore ${id}?`)) return;

                    try {
                        await apiPostForm("/api/manager/maintainers/delete", { id });
                        showAlert("Manutentore eliminato.");
                        loadMaintainers();
                    } catch (err) {
                        console.error(err);
                        showAlert("Errore eliminazione: " + err.message);
                    }
                });
            });

        } catch (err) {
            console.error(err);
            showAlert("Sessione non valida o errore server (manutentori).");
            window.location.href = "/login.html?err=session";
        }
    }

    async function loadDistributors() {
        distTbody.innerHTML = "";
        try {
            const data = await apiGetJSON("/api/manager/distributors/list");
            if (!data.ok) throw new Error("Risposta non valida");

            allDistributors = Array.isArray(data.items) ? data.items : [];
            renderDistributors(allDistributors);

        } catch (err) {
            console.error(err);
            showAlert("Sessione non valida o errore server (distributori).");
            window.location.href = "/login.html?err=session";
        }
    }

    // ------------------ SYNC MONITOR (bulk) ------------------

    async function syncMonitorNow() {
        try {
            const res = await fetch("/api/monitor/sync", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: "" // nessun parametro: il server legge dal DB principale
            });

            const txt = await res.text();
            let data = null;
            try { data = JSON.parse(txt); } catch (_) {}

            if (!res.ok) {
                const msg = (data && data.message) ? data.message : txt;
                showAlert("Sync fallita: " + msg);
                return;
            }

            if (data && data.ok) {
                showAlert(`Sync monitor completata. Distributori sincronizzati: ${data.count ?? 0}`);
            } else {
                showAlert("Sync monitor completata (risposta non standard).");
            }

            await loadDistributors();
            applyFilter();

        } catch (err) {
            console.error(err);
            showAlert("Errore sync monitor: " + err.message);
        }
    }

    // bind bottone sync
    const btnSync = document.getElementById("btn-sync-monitor");
    if (btnSync) {
        btnSync.addEventListener("click", async () => {
            if (!confirm("Sincronizzare ora tutti i distributori sul servizio Monitor?")) return;
            await syncMonitorNow();
        });
    }

    // ------------------ SEARCH ------------------

    btnSearch.addEventListener("click", () => applyFilter());

    btnAll.addEventListener("click", () => {
        searchInput.value = "";
        applyFilter();
    });

    searchInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            e.preventDefault();
            applyFilter();
        }
    });

    // ------------------ MODAL API (global) ------------------

    window.openModal = function (distId) {
        modalDistId.textContent = distId;
        modal.classList.remove("visually-hidden");
    };

    window.closeModal = function () {
        modal.classList.add("visually-hidden");
        currentDistIdForModal = null;
    };

    window.changeStatus = async function (uiStatus) {
        const id = currentDistIdForModal;
        if (!id) return;

        try {
            await apiPostForm("/api/manager/distributors/status", { id, stato: uiStatus });
            showAlert("Stato aggiornato!");
            window.closeModal();

            await loadDistributors();
            applyFilter();

        } catch (err) {
            console.error(err);
            showAlert("Errore aggiornamento stato: " + err.message);
        }
    };

    // ------------------ INIT ------------------

    loadMaintainers();
    loadDistributors().then(() => applyFilter());
});

function escapeHtml(s) {
    return (s ?? "").toString()
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}