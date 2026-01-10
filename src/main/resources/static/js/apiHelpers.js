// ============================================================
// Funzioni riusabili di supporto (GLOBAL, no modules)
// ============================================================

// funzione per recuperare i JSON
async function fetchJSON(url) {
    const res = await fetch(url, { cache: 'no-cache', credentials: 'same-origin' });
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
    return res.json();
}

// funzione per recuperare i testi semplici
async function fetchText(url) {
    const res = await fetch(url, { cache: 'no-cache', credentials: 'same-origin' });
    if (!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
    return res.text();
}

// funzione per recuperare gli XML, utilizziamo il fetch text e poi lo convertiamo in XML con il parser
async function fetchXML(url) {
    const txt = await fetchText(url);
    const parser = new DOMParser();
    return parser.parseFromString(txt, "application/xml");
}

// funzione generica di alert
function showAlert(msg) {
    alert(msg);
}

// prende un numero e lo trasforma in una stringa di prezzo formattata in euro
function formatCurrency(val) {
    const num = Number(val);
    if (isNaN(num)) return "0.00 €";
    return num.toFixed(2) + " €";
}

// ============================================================
// Cookie + CSRF helpers
// ============================================================

function getCookie(name) {
    const parts = document.cookie.split('; ');
    for (const p of parts) {
        const [k, ...rest] = p.split('=');
        if (k === name) return decodeURIComponent(rest.join('='));
    }
    return null;
}

function csrfHeader() {
    const token = getCookie('XSRF-TOKEN');
    // header richiesto da Spring Security per CookieCsrfTokenRepository
    return token ? { 'X-XSRF-TOKEN': token } : {};
}

async function parseResponse(res) {
    const ct = (res.headers.get('content-type') || '').toLowerCase();
    if (ct.includes('application/json')) return await res.json().catch(() => null);
    return await res.text().catch(() => null);
}

async function handleAuthErrors(res) {
    // 401 = non autenticato -> torna al login
    if (res.status === 401) {
        window.location.href = '/login.html';
        return true;
    }
    return false;
}

// ============================================================
// API helpers (per chiamare le servlet /api/...)
// ============================================================

// GET -> JSON o testo (lancia errore se non 2xx)
async function apiGet(url) {
    const res = await fetch(url, {
        method: 'GET',
        credentials: 'same-origin',
        cache: 'no-cache'
    });

    if (await handleAuthErrors(res)) return { ok: false, message: "unauthorized" };


    const payload = await parseResponse(res);

    if (!res.ok) {
        const msg = (payload && payload.message)
            ? payload.message
            : (payload || `${res.status} ${res.statusText}`);
        throw new Error(msg);
    }

    return payload;
}

// Compatibilità: nel tuo codice esistente chiami apiGetJSON
async function apiGetJSON(url) {
    return apiGet(url);
}

// POST application/x-www-form-urlencoded (compatibile con req.getParameter)
// -> include CSRF header automaticamente
async function apiPostForm(url, dataObj) {
    const body = new URLSearchParams();
    Object.entries(dataObj || {}).forEach(([k, v]) => body.append(k, v));

    const res = await fetch(url, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            ...csrfHeader()
        },
        body,
        cache: 'no-cache'
    });

    if (await handleAuthErrors(res)) return;

    const payload = await parseResponse(res);

    if (!res.ok) {
        const msg = (payload && payload.message)
            ? payload.message
            : (payload || `${res.status} ${res.statusText}`);
        throw new Error(msg);
    }

    return payload;
}

// POST JSON -> include CSRF header automaticamente
async function apiPostJson(url, obj) {
    const res = await fetch(url, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
            'Content-Type': 'application/json',
            ...csrfHeader()
        },
        body: JSON.stringify(obj || {}),
        cache: 'no-cache'
    });

    if (await handleAuthErrors(res)) return;

    const payload = await parseResponse(res);

    if (!res.ok) {
        const msg = (payload && payload.message)
            ? payload.message
            : (payload || `${res.status} ${res.statusText}`);
        throw new Error(msg);
    }

    return payload;
}
