document.addEventListener("DOMContentLoaded", () => {
    const btnLogout = document.getElementById("btn-logout");
    const btnRefresh = document.getElementById("btn-refresh");
    const info = document.getElementById("maintainer-info");
    const list = document.getElementById("list-distributors");

    // Base URL Monitor (WAR separata)
    const MONITOR_BASE_URL = "http://localhost:8081/CoffeMonitor_war_exploded";

    btnLogout.addEventListener("click", () => {
        window.location.href = "/route/logout";
    });

    btnRefresh.addEventListener("click", () => loadAll());

    async function loadMe() {
        try {
            const me = await apiGetJSON("/api/maintainer/me");
            if (me && me.ok) {
                info.textContent = "Connesso come: " + (me.username || "MAINTAINER");
                return;
            }
        } catch (e) {}
        window.location.href = "/login.html?err=session";
    }

    async function loadXml() {
        return fetchXML("/api/distributors/state.xml");
    }

    async function fetchMonitorMap() {
        try {
            const res = await fetch(`${MONITOR_BASE_URL}/api/monitor/map`, { method: "GET" });
            if (!res.ok) return null;
            const data = await res.json();
            if (!data || !data.ok || !Array.isArray(data.items)) return null;

            const map = new Map();
            for (const it of data.items) {
                if (!it || !it.code) continue;
                map.set(String(it.code), String(it.status || ""));
            }
            return map;
        } catch (e) {
            return null;
        }
    }

    function monitorStatusToUi(statusDb) {
        const s = String(statusDb || "").trim().toUpperCase();
        if (s === "FAULT") return "GUASTO";
        if (s === "MAINTENANCE") return "MANUTENZIONE";
        if (s === "ACTIVE") return "ATTIVO";
        return "";
    }

    function getText(node, selector) {
        const el = node.querySelector(selector);
        return el ? (el.textContent || "") : "";
    }

    async function renderDistributors(xmlDoc) {
        const nodes = Array.from(xmlDoc.querySelectorAll("distributore"));
        if (nodes.length === 0) {
            list.innerHTML = "<p>Nessun distributore trovato.</p>";
            return;
        }

        // Override stati dal Monitor anche qui
        const monitorMap = await fetchMonitorMap();

        list.innerHTML = nodes.map(d => {
            const code = d.getAttribute("id") || "-";
            const loc = getText(d, "locazione");
            const statoXml = getText(d, "stato_operativo");

            let stato = statoXml;
            if (monitorMap) {
                const stDb = monitorMap.get(String(code));
                const stUi = monitorStatusToUi(stDb);
                if (stUi) stato = stUi;
            }

            const cafe = getText(d, "livelli_forniture > caffe_gr");
            const latte = getText(d, "livelli_forniture > latte_lt");
            const zucch = getText(d, "livelli_forniture > zucchero_gr");
            const cups = getText(d, "livelli_forniture > bicchieri_num");

            const faults = Array.from(d.querySelectorAll("guasti > guasto")).map(g => {
                const c = getText(g, "codice");
                const desc = getText(g, "descrizione");
                const dt = getText(g, "data_rilevazione");
                return `<li><strong>${escapeHtml(c)}</strong> — ${escapeHtml(desc)} <small style="color:gray;">(${escapeHtml(dt)})</small></li>`;
            });

            return `
        <div style="border:1px solid #ddd; border-radius:12px; padding:14px; margin-bottom:12px;">
          <div style="display:flex; justify-content:space-between; gap:10px; flex-wrap:wrap;">
            <div>
              <h3 style="margin:0 0 6px;">${escapeHtml(code)}</h3>
              <div style="color:#555;">${escapeHtml(loc)}</div>
              <div style="margin-top:6px;">
                <strong>Stato:</strong> ${escapeHtml(stato)}
              </div>
            </div>

            <div style="min-width:280px;">
              <div><strong>Livelli</strong></div>
              <div style="color:#444; margin-top:6px;">
                Caffè: ${escapeHtml(cafe)} | Latte: ${escapeHtml(latte)} | Zucchero: ${escapeHtml(zucch)} | Bicchieri: ${escapeHtml(cups)}
              </div>

              <div style="display:flex; gap:8px; flex-wrap:wrap; margin-top:10px;">
                <button class="btn" data-action="refill" data-code="${escapeAttr(code)}">Ripristina livelli</button>

                <button class="btn btn-secondario" data-action="status" data-status="attivo" data-code="${escapeAttr(code)}">Attivo</button>
                <button class="btn btn-secondario" data-action="status" data-status="manutenzione" data-code="${escapeAttr(code)}">Manutenzione</button>
                <button class="btn btn-secondario" data-action="status" data-status="disattivo" data-code="${escapeAttr(code)}">Guasto</button>
              </div>
            </div>
          </div>

          <div style="margin-top:12px;">
            <strong>Guasti aperti</strong>
            ${faults.length ? `<ul style="margin:8px 0 0;">${faults.join("")}</ul>` : `<div style="color:gray; margin-top:6px;">Nessun guasto aperto</div>`}
          </div>
        </div>
      `;
        }).join("");

        list.querySelectorAll("button[data-action]").forEach(btn => {
            btn.addEventListener("click", async () => {
                const action = btn.getAttribute("data-action");
                const code = btn.getAttribute("data-code");
                if (!code) return;

                btn.disabled = true;

                try {
                    if (action === "refill") {
                        await apiPostForm("/api/maintainer/distributors/refill", { code });
                        showAlert("Livelli ripristinati per " + code);
                    } else if (action === "status") {
                        const status = btn.getAttribute("data-status");
                        await apiPostForm("/api/maintainer/distributors/status", { code, status });
                        showAlert("Stato aggiornato per " + code + " -> " + status);
                    }
                    await loadAll();
                } catch (e) {
                    console.error(e);
                    showAlert("Errore: " + (e.message || "operazione fallita"));
                } finally {
                    btn.disabled = false;
                }
            });
        });
    }

    async function loadAll() {
        try {
            const xml = await loadXml();
            await renderDistributors(xml);
        } catch (e) {
            console.error(e);
            showAlert("Errore caricamento XML distributori.");
        }
    }

    function escapeHtml(s) {
        return String(s ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#039;");
    }

    function escapeAttr(s) {
        return escapeHtml(s).replaceAll("`", "&#096;");
    }

    loadMe().then(loadAll);
});