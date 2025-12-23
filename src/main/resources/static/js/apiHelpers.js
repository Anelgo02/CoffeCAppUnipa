// funzioni riusabili di supporto

// funzione per recuperare i JSON
async function fetchJSON(url){
    const res = await fetch(url, { cache: 'no-cache' });
    if(!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
    return res.json();
}

// funzione per recuperare i testi semplici
async function fetchText(url){
    const res = await fetch(url, { cache: 'no-cache' });
    if(!res.ok) throw new Error(`HTTP ${res.status} ${res.statusText}`);
    return res.text();
}

// funzione per recuperare gli XML, utilizziamo il fetch text e poi lo convertiamo in XML con il parser
async function fetchXML(url) {
    const txt = await fetchText(url);
    const parser = new DOMParser();
    return parser.parseFromString(txt, "application/xml");
}

// funzione generica di alert
function showAlert(msg){
    alert(msg);
}

// prende un numero e lo trasforma in una stringa di prezzo formattata in euro
function formatCurrency(val) {
    const num = Number(val);
    if (isNaN(num)) return "0.00 €";
    return num.toFixed(2) + " €";
}


// ------------------------------------------------------------
// API helpers (per chiamare le servlet /api/...)
// ------------------------------------------------------------

// GET -> JSON (lancia errore se non 2xx)
async function apiGetJSON(url){
    const res = await fetch(url, { cache: "no-cache" });

    if (!res.ok) {
        const txt = await safeText(res);
        throw new Error(`HTTP ${res.status} ${res.statusText} - ${txt}`);
    }

    return res.json();
}

// POST application/x-www-form-urlencoded (compatibile con req.getParameter)
async function apiPostForm(url, dataObj){
    const body = new URLSearchParams();
    Object.entries(dataObj || {}).forEach(([k, v]) => body.append(k, v));

    const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body,
        cache: "no-cache"
    });

    // Se la risposta è JSON, leggiamo JSON, altrimenti testo
    const contentType = (res.headers.get("content-type") || "").toLowerCase();
    const payload = contentType.includes("application/json")
        ? await res.json().catch(() => null)
        : await res.text().catch(() => null);

    if (!res.ok) {
        const msg = (payload && payload.message)
            ? payload.message
            : (payload || `${res.status} ${res.statusText}`);
        throw new Error(msg);
    }

    return payload;
}

// utility: leggere testo in modo sicuro senza far esplodere il codice
async function safeText(res){
    try { return await res.text(); }
    catch { return ""; }
}
