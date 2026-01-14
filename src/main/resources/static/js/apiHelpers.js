// ============================================================
// Funzioni riusabili di supporto (GLOBAL, no modules)
// ============================================================

// --- Costanti sicurezza distributore ---
const DIST_TOKEN_KEY = "distributor_token";
const DIST_ID_KEY = "distributor_identity";
const DIST_HEADER = "X-Distributor-Auth";

function getDistributorToken() {
    const t = localStorage.getItem(DIST_TOKEN_KEY);
    return (t && typeof t === "string" && t.trim().length > 0) ? t.trim() : null;
}

function resetDistributorIdentity() {
    localStorage.removeItem(DIST_ID_KEY);
    localStorage.removeItem(DIST_TOKEN_KEY);
}

function isDistributorContext() {
    const pathname = window.location.pathname;

    return pathname === "/distributore" || pathname.startsWith("/distributore/");
}

function isBootPage() {
    return window.location.pathname.endsWith("/boot.html");
}

function goToDistributorBoot() {
    resetDistributorIdentity();
    window.location.href = "/distributore/boot.html";
}

// ============================================================
// Funzione generica per gestire la risposta fetch
// ============================================================

async function handleFetchResponse(res) {
    // 1) Auth errors
    if (res.status === 401 || res.status === 403) {
        console.warn(`Accesso negato (${res.status}). URL: ${res.url}`);

        // Distributore: token invalido -> reset + boot
        if (isDistributorContext()) {
            if (!isBootPage()) {
                console.error("Token distributore invalido/scaduto. Reset + redirect BOOT.");
                goToDistributorBoot();
            }
            // In boot: ritorniamo body per mostrare messaggio
            return await res.json().catch(() => null);
        }

        // Altri ruoli: login
        window.location.href = "/login.html";
        return null;
    }

    // 2) Parse body
    const ct = (res.headers.get("content-type") || "").toLowerCase();
    let payload = null;

    try {
        if (ct.includes("application/json")) payload = await res.json();
        else payload = await res.text();
    } catch (e) {
        console.warn("Errore parsing response:", e);
    }

    // 3) HTTP errors
    if (!res.ok) {
        const msg =
            (payload && payload.message) ? payload.message :
                (typeof payload === "string" && payload.length > 0) ? payload :
                    `Errore HTTP ${res.status}: ${res.statusText}`;

        throw new Error(msg);
    }

    return payload;
}

// ============================================================
// Helper per aggiungere parametri anti-cache
// ============================================================

function noCacheUrl(url) {
    const separator = url.includes("?") ? "&" : "?";
    return `${url}${separator}_=${Date.now()}`;
}

// ============================================================
// Cookie + CSRF helpers
// ============================================================

function getCookie(name) {
    const parts = document.cookie.split("; ");
    for (const p of parts) {
        const [k, ...rest] = p.split("=");
        if (k === name) return decodeURIComponent(rest.join("="));
    }
    return null;
}

function csrfHeader() {
    const token = getCookie("XSRF-TOKEN");
    return token ? { "X-XSRF-TOKEN": token } : {};
}

// ============================================================
// CORE: fetch wrapper che aggiunge token distributore
// ============================================================

async function apiFetch(url, options = {}) {
    const token = getDistributorToken();

    // Headers merge robusto
    const headers = new Headers(options.headers || {});

    // Token distributore (solo se presente)
    if (token) {
        headers.set(DIST_HEADER, token);
    }

    // Default credentials: same-origin (cookie session per utenti loggati)
    const finalOptions = {
        credentials: "same-origin",
        ...options,
        headers
    };

    const res = await fetch(url, finalOptions);
    return handleFetchResponse(res);
}

// ============================================================
// API helpers (per chiamare le servlet /api/...)
// ============================================================

// GET
async function apiGet(url) {
    return apiFetch(noCacheUrl(url), { method: "GET" });
}

// Compatibilità
const apiGetJSON = apiGet;
const fetchJSON = apiGet;

// POST form urlencoded
async function apiPostForm(url, dataObj) {
    const body = new URLSearchParams();
    Object.entries(dataObj || {}).forEach(([k, v]) => body.append(k, v));

    return apiFetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            ...csrfHeader()
        },
        body
    });
}

// POST json
async function apiPostJson(url, obj) {
    return apiFetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...csrfHeader()
        },
        body: JSON.stringify(obj || {})
    });
}

// ============================================================
// Utility varie
// ============================================================

function showAlert(msg) {
    alert(msg);
}

function formatCurrency(val) {
    const num = Number(val);
    if (isNaN(num)) return "0.00 €";
    return num.toFixed(2) + " €";
}

async function fetchXML(url) {
    const res = await fetch(noCacheUrl(url), { credentials: "same-origin" });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const txt = await res.text();
    const parser = new DOMParser();
    return parser.parseFromString(txt, "application/xml");
}
