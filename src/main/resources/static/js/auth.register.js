/**
 * Gestione Registrazione Cliente con Auto-Login
 */

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("form-register");
    if (form) {
        form.addEventListener("submit", handleRegister);
    }
});

async function handleRegister(event) {
    event.preventDefault();

    const username = document.getElementById("reg-username").value.trim();
    const email = document.getElementById("reg-email").value.trim();
    const password = document.getElementById("reg-password").value.trim();
    const errorBox = document.getElementById("reg-error");
    const btn = document.getElementById("btn-register");

    // Reset UI
    errorBox.style.display = "none";
    errorBox.textContent = "";

    if (!username || !email || !password) {
        showError("Compila tutti i campi.");
        return;
    }

    btn.disabled = true;
    btn.textContent = "Registrazione in corso...";

    try {
        // 1. CHIAMATA DI REGISTRAZIONE (Crea l'utente nel DB)
        // La Servlet CustomerServlet si aspetta parametri form (getParameter), non JSON body puro.
        const registerParams = new URLSearchParams();
        registerParams.append("username", username);
        registerParams.append("email", email);
        registerParams.append("password", password);

        const regResp = await fetch("/api/customer/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "X-XSRF-TOKEN": getCsrfToken() // Token anti-CSRF
            },
            body: registerParams
        });

        const regData = await regResp.json();

        if (!regResp.ok || !regData.ok) {
            throw new Error(regData.message || "Errore durante la registrazione.");
        }

        // 2. AUTO-LOGIN (Crea la sessione Spring Security)
        // Usiamo le stesse credenziali per fare il login automatico
        btn.textContent = "Accesso in corso...";

        const loginParams = new URLSearchParams();
        loginParams.append("username", username);
        loginParams.append("password", password);

        const loginResp = await fetch("/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                "X-XSRF-TOKEN": getCsrfToken()
            },
            body: loginParams
        });

        // Spring Security risponde con un redirect (302) in caso di successo,
        // oppure con errore (es. se username errato). Fetch segue i redirect automaticamente.
        // Se siamo finiti sulla pagina di login con errore (?err), qualcosa è andato storto.
        if (loginResp.url.includes("error") || loginResp.url.includes("err=")) {
            throw new Error("Registrazione riuscita, ma login automatico fallito. Prova ad accedere manualmente.");
        }

        // 3. SUCCESS -> Redirect alla Dashboard
        window.location.href = "/cliente/index.html";

    } catch (err) {
        console.error(err);
        showError(err.message);
        btn.disabled = false;
        btn.textContent = "Registrati";
    }
}

function showError(msg) {
    const errorBox = document.getElementById("reg-error");
    errorBox.textContent = msg;
    errorBox.style.display = "block";
}

// Helper per leggere il cookie CSRF scritto dal backend (CsrfCookieFilter)
function getCsrfToken() {
    const name = "XSRF-TOKEN=";
    const decodedCookie = decodeURIComponent(document.cookie);
    const ca = decodedCookie.split(';');
    for(let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}