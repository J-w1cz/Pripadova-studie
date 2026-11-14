Přehled ekonomických subjektů v ČR

Jednoduchá webová aplikace pro procházení ekonomických subjektů z Registru ekonomických subjektů (RES) ČSÚ a vyhledávání subjektů přes ARES. Jde o prototyp vytvořený jako technické assessment zadání.

## Co aplikace umí

- Zobrazit subjekty načtené z RES ČSÚ po stránkách (25 položek).
- Filtrovat lokální seznam podle obce a omezit jej na aktivní subjekty.
- Vyhledávat název a IČO přes online API ARES; tyto dotazy nejdou do lokální databáze RES.
- Zobrazit základní údaje dostupné v RES: IČO, název, obec/sídlo, právní formu, datum vzniku, kategorii počtu zaměstnanců a další údaje v API.
- Otevřít záznam subjektu na externím webu Justice kliknutím na řádek.

## Použité technologie

- Java 21, Spring Boot 3, Maven
- H2 embedded databáze v souborovém režimu
- HTML, CSS a JavaScript
- Apache Commons CSV pro import dat

## Zdroje a rozsah dat

- **RES ČSÚ** je zdrojem pro tabulku, lokální filtr obce a detailní API záznam. Aplikace při prvním spuštění stáhne CSV snapshot a importuje jej do H2. Data odpovídají datu platnosti uvedenému ve snapshotu; ČSÚ publikuje aktualizované snapshoty.
- **ARES** slouží pro online vyhledávání podle názvu a IČO. Výsledky hledání v ARES nejsou ukládány do lokální databáze.
- RES není seznamem pouze obchodních společností. Zahrnuje širší soubor ekonomických subjektů, včetně podnikajících fyzických osob a dalších subjektů.

Zdroje: [CSV ČSÚ RES](https://opendata.csu.gov.cz/soubory/od/od_org03/res_data.csv), [dokumentace otevřených dat RES](https://csu.gov.cz/registr-ekonomickych-subjektu-otevrena-data-dokumentace), [ARES](https://ares.gov.cz/).

## Spuštění lokálně

Požadavky: JDK 21, Maven a internetové připojení při prvním spuštění.

```bash
mvn spring-boot:run
```

Poté otevřete <http://localhost:8080>. Při prvním startu aplikace stáhne CSV do `data/res_data.csv` a naimportuje jej do H2 databáze `data/resdb`. Import velkého snapshotu může první spuštění výrazně prodloužit. Při dalších spuštěních se import přeskočí, pokud metadata databáze označují import jako dokončený. Soubory v `data/` jsou lokální provozní data, nikoli zdrojový kód.

## API

- `GET /api/companies?location=Praha&activeOnly=true&page=0&size=25` – stránka RES, volitelný prefix obce a filtr aktivních subjektů.
- `GET /api/companies/search?type=municipality&query=Praha&activeOnly=true&page=0&size=25` – vyhledávání v RES podle obce. `type` může být také `name` nebo `ico`; tyto dvě varianty hledají online přes ARES.
- `GET /api/companies/ares/search?query=Koala&page=0&size=25` – přímé online hledání v ARES.
- `GET /api/companies/{ico}` – základní detail subjektu z lokálního RES.
- `GET /api/companies/stats` – celkový počet záznamů, počet aktivních a datum platnosti dat.

## Co zadání zatím nepokrývá

- **Obrat:** aplikace nemá filtr podle obratu. Ve zvoleném snapshotu a implementovaném datovém modelu není obrat použit.
- **Vlastníci, management, telefon a e-mail:** tyto osoby ani kontaktní údaje aplikace nezískává ani nezobrazuje. Vyžadovalo by to další vhodné zdroje, ověření jejich licenčních a provozních podmínek a propojení dat podle IČO.
- **Detail v aplikaci:** API vrací základní údaje z RES, ale tabulka otevírá externí stránku Justice; úplná detailní obrazovka není implementována.

## Známá omezení a technický dluh

- „Pouze aktivní“ znamená, že záznam nemá datum zániku (`DDATZAN`). Jde o jednoduché odvození podle dostupného pole.
- Kategorie zaměstnanců je interval/kategorie, nikoli přesný počet. Některá pole mohou být prázdná.
- RES snapshot se po prvním importu automaticky neobnovuje. Pro další běh je potřeba smazat nebo aktualizovat lokální databázi podle bezpečnějšího importního postupu.
- Import zatím nemá plnohodnotný atomický refresh ani dostatečně ošetřené opakování při chybě stahování. Pro produkční provoz by bylo potřeba doplnit obnovu dat, monitoring, testy mapování a robustnější práci s databází.
- Vyhledávání názvu a IČO závisí na dostupnosti a limitech ARES.


## Časová náročnost

Na zpracování této verze jsem strávil přibližně 4–5 hodin.
