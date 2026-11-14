
const form = document.querySelector("#search-form");
const input = document.querySelector("#query");
const button = document.querySelector("#search-button");
const results = document.querySelector("#results");
const statusBox = document.querySelector("#status");
const caption = document.querySelector("#result-caption");
const count = document.querySelector("#count");
const mode = document.querySelector("#search-type");
const activeOnly = document.querySelector("#active-only");
const empty = document.querySelector("#empty");
const prev = document.querySelector("#prev-page");
const next = document.querySelector("#next-page");
const pageLabel = document.querySelector("#page-label");

const queryLabel = document.querySelector("#query-label");
const activeFilter = document.querySelector("#active-filter");

let page = 0;
const pageSize = 25;
let hasNext = false;
let currentType = "municipality";

// Řízení požadavků
let currentController = null;
let requestId = 0;

function updateSearchMode() {
    const type = mode.value;

    const settings = {
        name: {
            label: "Název společnosti",
            placeholder: "Zadejte název společnosti"
        },
        ico: {
            label: "IČO",
            placeholder: "Zadejte osmimístné IČO"
        },
        municipality: {
            label: "Obec",
            placeholder: "Zadejte název obce"
        }
    };

    const selected = settings[type];

    queryLabel.textContent = selected.label;
    input.placeholder = selected.placeholder;
    input.inputMode = type === "ico" ? "numeric" : "text";

    const isMunicipality = type === "municipality";

    activeFilter.classList.toggle("hidden", !isMunicipality);
    activeOnly.disabled = !isMunicipality;
}

mode.addEventListener("change", () => {
    page = 0;
    currentType = mode.value;

    updateSearchMode();
    loadCompanies();
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    page = 0;
    currentType = mode.value;

    await loadCompanies();
});

activeOnly.addEventListener("change", () => {
    if (currentType === "municipality") {
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

async function loadCompanies(isUserInitiated = true) {

    // Zrušení předchozího požadavku.
    if (currentController) {
        currentController.abort();
    }

    const controller = new AbortController();
    currentController = controller;

    const thisRequestId = ++requestId;

    const query = input.value.trim();

    // Tlačítko blokujeme jen při uživatelském hledání.
    // Prvotní automatické načítání ho nechává dostupné.
    if (isUserInitiated) {
        button.disabled = true;
        button.textContent = "Načítám…";
    } else {
        button.disabled = false;
        button.textContent = "Vyhledat";
    }

    statusBox.className = "status";
    statusBox.textContent = "";

    results.replaceChildren();
    empty.hidden = true;
    count.textContent = "…";

    prev.disabled = true;
    next.disabled = true;

    if (!isUserInitiated) {
        caption.textContent = "Načítám data z registru...";
    }

    try {
        if (
            (currentType === "name" || currentType === "ico")
            && !query
        ) {
            throw new Error("Nejdříve zadejte vyhledávací dotaz.");
        }

        if (currentType === "ico" && !/^\d{8}$/.test(query)) {
            throw new Error("IČO musí obsahovat přesně 8 číslic.");
        }

        const params = new URLSearchParams({
            type: currentType,
            query,
            activeOnly: activeOnly.checked,
            page,
            size: pageSize
        });

        const response = await fetch(
            `/api/companies/search?${params}`,
            {
                signal: controller.signal
            }
        );

        if (!response.ok) {
            throw new Error(await getError(response));
        }

        const data = await response.json();

        // Pokud mezitím začal nový požadavek,
        // starý nesmí měnit UI.
        if (thisRequestId !== requestId) {
            return;
        }

        const items = data.items || [];
        hasNext = Boolean(data.hasNext);

        items.forEach(renderRow);

        count.textContent = items.length.toLocaleString("cs-CZ");

        let sourceDescription;

        switch (currentType) {
            case "name":
                sourceDescription = `ARES: výsledky pro „${query}“.`;
                break;

            case "ico":
                sourceDescription = `ARES: výsledky pro IČO ${query}.`;
                break;

            case "municipality":
                sourceDescription =
                    `ČSÚ RES${query ? `, obec: ${query}` : ""}.`;
                break;
        }

        caption.textContent =
            `${sourceDescription} Zobrazeno ${items.length} subjektů na této stránce.`;

        empty.hidden = items.length > 0;

        pageLabel.textContent = `Stránka ${page + 1}`;

        prev.disabled = page === 0;
        next.disabled = !hasNext;

    } catch (error) {

        // Zrušený požadavek není chyba.
        if (error.name === "AbortError") {
            return;
        }

        // Starý požadavek nesmí přepsat aktuální stav.
        if (thisRequestId !== requestId) {
            return;
        }

        statusBox.className = "status error";
        statusBox.textContent =
            `Data se nepodařilo načíst: ${error.message}`;

        count.textContent = "0";
        empty.hidden = false;

        hasNext = false;
        prev.disabled = true;
        next.disabled = true;

    } finally {

        // UI ovládá pouze poslední aktivní požadavek.
        if (thisRequestId === requestId) {
            button.disabled = false;
            button.textContent = "Vyhledat";
            currentController = null;
        }
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
        formatEmployeeCategory(company.employeeCategory)
    ];

    values.forEach((value, index) => {
        const td = document.createElement("td");

        td.textContent = value;

        if (index === 0) td.className = "company-name";
        if (index === 1) td.className = "ico";

        tr.append(td);
    });

    if (company.ico) {
        tr.classList.add("clickable-row");
        tr.title = "Otevřít záznam společnosti na Justici";

        tr.addEventListener("click", () => {
            const justiceUrl =
                `https://or.justice.cz/ias/ui/rejstrik-$firma?ico=${encodeURIComponent(company.ico)}`;

            window.open(justiceUrl, "_blank", "noopener,noreferrer");
        });
    }

    results.append(tr);
}

function formatEmployeeCategory(code) {
    const categories = {
        "000": "Neuvedeno",
        "110": "Bez zaměstnanců",
        "120": "1–5",
        "130": "6–9",
        "210": "10–19",
        "220": "20–24",
        "230": "25–49",
        "240": "50–99",
        "310": "100–199",
        "320": "200–249",
        "330": "250–499",
        "340": "500–999",
        "410": "1 000–1 499",
        "420": "1 500–1 999",
        "430": "2 000–2 499",
        "440": "2 500–2 999",
        "450": "3 000–3 999",
        "460": "4 000–4 999",
        "470": "5 000–9 999",
        "510": "10 000 a více"
    };

    if (!code) return "—";

    const normalizedCode = String(code).padStart(3, "0");

    return categories[normalizedCode]
        ? `${categories[normalizedCode]} zaměstnanců`
        : "Neznámá kategorie";
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

// Inicializace
updateSearchMode();

// Automatické načtení zachováváme,
// ale tlačítko zůstává dostupné.
loadCompanies(false);
