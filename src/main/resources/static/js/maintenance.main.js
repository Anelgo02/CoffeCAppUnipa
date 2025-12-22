const STATUS_XML = "../data/esempio_stato.xml";
const lista_bevande = ["caffe_gr","latte_lt","cioccolata_gr","te_gr","zucchero_gr","bicchieri_num"];

function createStatusHtml(distributorNode){
    //riceve come argomento un elemento XML distributorNode

    const id = distributorNode.getAttribute("id");
    const loc=distributorNode.querySelector("locazione")?.textContent || '';
    const stato=distributorNode.querySelector("stato_operativo").textContent || '';

    //creo il codice HTML che andro' a inserire nella pagina principale

    let html = `<h3>Distributore ${id}</h3>`;
    html += `<p>Stato Operativo: ${stato}</p>`;
    html += `<p>Locazione: ${loc}</p>`;

    const livelli = distributorNode.querySelector("livelli_forniture");
    if(livelli){
        //se ci sono livelli di forniture creo la lista
        html += `<ul>`;
        //per ogni bevanda presente appendo l'elemento nella lista
        lista_bevande.forEach(tag => {
            const el = livelli.querySelector(tag);
            if(el) {
                html += `<li>${tag}: ${el.textContent}</li>`;
            }
        });
        html += `</ul>`;
    }

    //guasti
    const guasti = distributorNode.querySelectorAll("guasti > guasto");
    if(guasti.length){
        html += `<h4>Guasti</h4><ul>`;
        guasti.forEach(guasto =>{
            html += `<li>Codice del guasto: ${guasto.querySelector("codice")?.textContent || ''} - ${guasto.querySelector("descrizione")?.textContent || ''} (${guasto.querySelector("data_rilevazione")?.textContent || ''})</li>`
        });
        html += `</ul>`;

    }else{
        html += `<p>Nessun guasto segnalato.</p>`;
    }
    return html;

}


// Logout manuale
document.getElementById("btn-logout").addEventListener("click", () => {
    localStorage.removeItem("loggedUser");
    window.location.href = "../login.html";
});

//listener per il bottone
document.getElementById("btn-load-state").addEventListener("click", async () =>{
    const id=document.getElementById("dist-id").value.trim();//prendo il valore id del distributore
    if(!id){
        showAlert("Inserisci l'Id del distributore"); return;
    }

    try{
        const xmlDoc = await fetchXML(STATUS_XML);
        //ricerco il distributore
        const distributor = xmlDoc.getElementsByTagName("distributore");
        let found = null;
        for (let d of distributor) {
            if(d.getAttribute("id") === id){
                found=d; break;
            }
        }

        const out = document.getElementById("maint-result");
        if(!found){
            out.innerHTML = `Distributore con id: ${id} non trovato`;
        }else{
            out.innerHTML = createStatusHtml(found);
        }
    }catch(e){
        console.error(e);
        showAlert("Errore nel caricamento del file XML");
    }
});