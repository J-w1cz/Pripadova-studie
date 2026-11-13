const form = document.querySelector("#search-form");
const input = document.querySelector("#query");
const button = document.querySelector("#search-button");
const results = document.querySelector("#results");
const statusBox = document.querySelector("#status");
const caption = document.querySelector("#result-caption");
const count = document.querySelector("#count");
const mode = document.querySelector("#search-mode");
const activeOnly = document.querySelector("#active-only");
const empty = document.querySelector("#empty");
const prev = document.querySelector("#prev-page");
const next = document.querySelector("#next-page");
const pageLabel = document.querySelector("#page-label");

let page = 0;
const pageSize = 25;
let pageCount = 0;
let currentMode = "csu";

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  page = 0;
  currentMode = mode.value;
  await loadCompanies();
});

activeOnly.addEventListener("change", () => {
  if (currentMode === "csu") { page = 0; loadCompanies(); }
});
prev.addEventListener("click", () => { if (page > 0) { page--; loadCompanies(); } });
next.addEventListener("click", () => { if (page + 1 < pageCount) { page++; loadCompanies(); } });

async function loadCompanies() {
  const query = input.value.trim();
  button.disabled = true;
  button.textContent = "Načítám…";
  statusBox.className = "status";
  statusBox.textContent = "";
  results.replaceChildren();
  empty.hidden = true;
  count.textContent = "…";

  try {
    let items;
    let total;
    if (currentMode === "ares") {
      if (!query) throw new Error("Pro online vyhledávání ARES zadejte název nebo IČO.");
      const response = await fetch(`/api/companies/ares/search?query=${encodeURIComponent(query)}`);
      if (!response.ok) throw new Error(await getError(response));
      items = await response.json();
      total = items.length;
      pageCount = 1;
      caption.textContent = `ARES: nalezeno ${total} subjektů pro „${query}“.`;
    } else {
      const params = new URLSearchParams({query, activeOnly: activeOnly.checked, page, size: pageSize});
      const response = await fetch(`/api/companies?${params}`);
      if (!response.ok) throw new Error(await getError(response));
      const data = await response.json();
      items = data.items;
      total = data.total;
      pageCount = data.pages;
      caption.textContent = `${total.toLocaleString("cs-CZ")} záznamů v databázi ČSÚ${query ? ` pro „${query}“` : ""}.`;
    }

    count.textContent = total.toLocaleString("cs-CZ");
    items.forEach(renderRow);
    empty.hidden = items.length > 0;
    pageLabel.textContent = currentMode === "ares" ? "ARES výsledky" : `Stránka ${page + 1} z ${Math.max(pageCount, 1)}`;
    prev.disabled = currentMode === "ares" || page <= 0;
    next.disabled = currentMode === "ares" || page + 1 >= pageCount;
  } catch (error) {
    statusBox.className = "status error";
    statusBox.textContent = `Data se nepodařilo načíst: ${error.message}`;
    count.textContent = "0";
    empty.hidden = false;
  } finally {
    button.disabled = false;
    button.textContent = "Vyhledat";
  }
}

function renderRow(company) {
  const tr = document.createElement("tr");
  const values = [
    company.name || "—",
    company.ico || "—",
    company.municipality || company.address || "—",
    company.legalForm || "—",
    formatDate(company.established),
    company.employeeCategory || "—"
  ];
  values.forEach((value, index) => {
    const td = document.createElement("td");
    td.textContent = value;
    if (index === 0) td.className = "company-name";
    if (index === 1) td.className = "ico";
    tr.append(td);
  });
  results.append(tr);
}

function formatDate(value) {
  if (!value) return "—";
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  return match ? `${match[3]}.${match[2]}.${match[1]}` : value;
}

async function getError(response) {
  const body = await response.json().catch(() => ({}));
  return body.error || body.message || `HTTP ${response.status}`;
}

// Initial render: list page from the ČSÚ snapshot.
loadCompanies();
