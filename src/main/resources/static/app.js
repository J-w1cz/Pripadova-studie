
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
let hasNext = false;
let currentMode = "csu";

const queryLabel = document.querySelector("#query-label");
const activeFilter = document.querySelector("#active-filter");

function updateSearchMode() {
  const isAres = mode.value === "ares";

  queryLabel.textContent = isAres
    ? "Vyhledat firmu"
    : "Filtrovat podle obce";

  input.placeholder = isAres
    ? "Název firmy nebo IČO"
    : "Zadejte název obce";

  activeFilter.classList.toggle("hidden", isAres);
  activeOnly.disabled = isAres;
}

mode.addEventListener("change", () => {
  page = 0;
  currentMode = mode.value;

  updateSearchMode();

  loadCompanies();
});

updateSearchMode();


form.addEventListener("submit", async (event) => {
  event.preventDefault();

  page = 0;
  currentMode = mode.value;

  await loadCompanies();
});

activeOnly.addEventListener("change", () => {
  if (currentMode === "csu") {
    page = 0;
    loadCompanies();
  }
});

prev.addEventListener("click", () => {
  if (page > 0) {
    page--;
    loadCompanies();
  }
});

next.addEventListener("click", () => {
  if (hasNext) {
    page++;
    loadCompanies();
  }
});

async function loadCompanies() {
  const query = input.value.trim();

  button.disabled = true;
  button.textContent = "Načítám…";

  statusBox.className = "status";
  statusBox.textContent = "";

  results.replaceChildren();
  empty.hidden = true;
  count.textContent = "…";

  prev.disabled = true;
  next.disabled = true;

  try {
    let items;
    let sourceDescription;

    if (currentMode === "ares") {
      if (!query) {
        throw new Error("Pro online vyhledávání ARES zadejte název nebo IČO.");
      }

      const params = new URLSearchParams({
        query,
        page,
        size: pageSize
      });

      const response = await fetch(
        `/api/companies/ares/search?${params}`
      );

      if (!response.ok) {
        throw new Error(await getError(response));
      }

      const data = await response.json();

      // Podpora nového formátu API.
      // Druhá větev zachovává kompatibilitu se starým API,
      // které vracelo přímo pole.
      if (Array.isArray(data)) {
        items = data;
        hasNext = false;
      } else {
        items = data.items || [];
        hasNext = Boolean(data.hasNext);
      }

      sourceDescription = `ARES: výsledky pro „${query}“.`;

    } else {
      const params = new URLSearchParams({
        location: query,
        activeOnly: activeOnly.checked,
        page,
        size: pageSize
      });

      const response = await fetch(`/api/companies?${params}`);

      if (!response.ok) {
        throw new Error(await getError(response));
      }

      const data = await response.json();

      items = data.items || [];
      hasNext = Boolean(data.hasNext);

      sourceDescription =
        `Databáze ČSÚ${query ? `, obec: ${query}` : ""}.`;
    }

    items.forEach(renderRow);

    count.textContent = items.length.toLocaleString("cs-CZ");

    caption.textContent =
      `${sourceDescription} Zobrazeno ${items.length} subjektů na této stránce.`;

    empty.hidden = items.length > 0;

    pageLabel.textContent = `Stránka ${page + 1}`;

    prev.disabled = page === 0;
    next.disabled = !hasNext;

  } catch (error) {
    statusBox.className = "status error";
    statusBox.textContent =
      `Data se nepodařilo načíst: ${error.message}`;

    count.textContent = "0";
    empty.hidden = false;

    hasNext = false;
    prev.disabled = true;
    next.disabled = true;

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

  return match
    ? `${match[3]}.${match[2]}.${match[1]}`
    : value;
}

async function getError(response) {
  const body = await response.json().catch(() => ({}));

  return body.error ||
    body.message ||
    `HTTP ${response.status}`;
}

// Úvodní načtení první stránky ČSÚ.
loadCompanies();
