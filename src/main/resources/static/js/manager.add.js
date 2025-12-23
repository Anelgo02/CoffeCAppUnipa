document.addEventListener("DOMContentLoaded", () => {
    const btnSaveMan = document.getElementById("btn-save-man");
    const btnSaveDist = document.getElementById("btn-save-dist");

    // ----------------- ADD MAINTAINER -----------------
    if (btnSaveMan) {
        btnSaveMan.addEventListener("click", async () => {
            const id = (document.getElementById("m-id").value || "").trim();
            const nome = (document.getElementById("m-nome").value || "").trim();
            const cognome = (document.getElementById("m-cognome").value || "").trim();
            const telefono = (document.getElementById("m-telefono").value || "").trim();
            const email = (document.getElementById("m-email").value || "").trim();

            if (!id || !nome || !cognome || !telefono || !email) {
                showAlert("Compila tutti i campi del manutentore.");
                return;
            }

            try {
                await apiPostForm("/api/manager/maintainers/create", {
                    id, nome, cognome, telefono, email
                });

                showAlert("Manutentore salvato!");
                window.location.href = "index.html";

            } catch (err) {
                console.error(err);
                showAlert("Errore salvataggio manutentore: " + err.message);
            }
        });
    }

    // ----------------- ADD DISTRIBUTOR -----------------
    if (btnSaveDist) {
        btnSaveDist.addEventListener("click", async () => {
            const id = (document.getElementById("d-id").value || "").trim();
            const locazione = (document.getElementById("d-loc").value || "").trim();
            const stato = (document.getElementById("d-stato").value || "").trim(); // ATTIVO / IN MANUTENZIONE / DISATTIVO

            if (!id || !locazione || !stato) {
                showAlert("Compila tutti i campi del distributore.");
                return;
            }

            try {
                await apiPostForm("/api/manager/distributors/create", {
                    id,
                    locazione,
                    stato
                });

                showAlert("Distributore salvato!");
                window.location.href = "index.html";

            } catch (err) {
                console.error(err);
                showAlert("Errore salvataggio distributore: " + err.message);
            }
        });
    }
});
