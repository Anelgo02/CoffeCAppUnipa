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

    // Logout (hai già link diretto, ok così)
    if (btnLogout) {
        btnLogout.addEventListener("click", (e) => {
            // se vuoi invalidare sessione via servlet:
            // e.preventDefault(); window.location.href = "/route/logout";
        });
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

            // bind delete
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

    async function loadDistributors(query) {
        distTbody.innerHTML = "";
        try {
            const url = query ? `/api/manager/distributors/list?q=${encodeURIComponent(query)}` : "/api/manager/distributors/list";
            const data = await apiGetJSON(url);
            if (!data.ok) throw new Error("Risposta non valida");

            data.items.forEach((d) => {
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
                    openModal(currentDistIdForModal);
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
                        loadDistributors(searchInput.value.trim());
                    } catch (err) {
                        console.error(err);
                        showAlert("Errore eliminazione: " + err.message);
                    }
                });
            });

        } catch (err) {
            console.error(err);
            showAlert("Sessione non valida o errore server (distributori).");
            window.location.href = "/login.html?err=session";
        }
    }

    // ------------------ SEARCH ------------------

    btnSearch.addEventListener("click", () => {
        const q = (searchInput.value || "").trim();
        loadDistributors(q);
    });

    btnAll.addEventListener("click", () => {
        searchInput.value = "";
        loadDistributors("");
    });

    // ------------------ MODAL API (chiamate global per HTML onclick) ------------------

    window.openModal = function (distId) {
        modalDistId.textContent = distId;
        modal.classList.remove("visually-hidden");
    };

    window.closeModal = function () {
        modal.classList.add("visually-hidden");
        currentDistIdForModal = null;
    };

    window.changeStatus = async function (uiStatus) {
        // UI buttons passano: ATTIVO / MANUTENZIONE / DISATTIVO (tu hai scritto MANUTENZIONE)
        const id = currentDistIdForModal;
        if (!id) return;

        try {
            await apiPostForm("/api/manager/distributors/status", { id, stato: uiStatus });
            showAlert("Stato aggiornato!");
            closeModal();
            loadDistributors((searchInput.value || "").trim());
        } catch (err) {
            console.error(err);
            showAlert("Errore aggiornamento stato: " + err.message);
        }
    };

    // ------------------ INIT ------------------
    loadMaintainers();
    loadDistributors("");
});

function escapeHtml(s) {
    return (s ?? "").toString()
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}
