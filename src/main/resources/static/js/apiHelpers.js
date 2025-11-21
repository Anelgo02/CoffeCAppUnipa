//funzioni riusabili di supporto


//funzione per recuperare i JSON
async function fetchJSON(url){
    const res=await fetch(url, {cache: 'no-cache'});
    if(!res.ok)throw new Error(`HTTP ${res.status} ${res.statusText}`);
    return res.json();
}

//funzione per recuperare i testi semplici
async function fetchText(url){
    const res=await fetch(url, {cache: 'no-cache'});
    if(!res.ok)throw new Error(`HTTP ${res.status} ${res.statusText}`);
    return res.text();
}

//funzione per recuperare gli XML, utilizziamo il fetch text e poi lo convertiamo in XML con il parser
async function fetchXML(url) {
    const txt = await fetchText(url);
    const parser = new DOMParser();
    return parser.parseFromString(txt,"application/xml");

}

//funzione generica di alert nel caso in cui volessi poi sostituire gli alert con dei pop-up personalizzati
function showAlert(msg){
    alert(msg);
}

//prende un numero e lo trasforma in una stringa di prezzo formattata in euro
function formatCurrency(val) {
    const num = Number(val);
    if (isNaN(num)) return "0.00 €";
    return num.toFixed(2) + " €";
}







