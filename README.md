# Případová studie – přehled ekonomických subjektů

Webová aplikace pro prohlížení ekonomických subjektů v České republice.

## Technologie
- Java 21
- Spring Boot 3
- H2 embedded databáze (souborová, lokální)
- HTML, CSS a JavaScript
- CSV import přes Apache Commons CSV

## Zdroje dat
- **ČSÚ RES otevřená data** tvoří hlavní databázi tabulky. Aplikace stáhne oficiální CSV snapshot při prvním spuštění a importuje ho do lokální H2 databáze. ČSÚ aktualizuje snapshoty dvakrát měsíčně.
- **ARES** se používá pouze pro online vyhledávání subjektů (režim „ARES · online vyhledávání“). ARES není hlavním zdrojem dat pro tabulku.

CSV zdroj: https://opendata.csu.gov.cz/soubory/od/od_org03/res_data.csv
Dokumentace ČSÚ: https://csu.gov.cz/registr-ekonomickych-subjektu-otevrena-data-dokumentace
ARES: https://ares.gov.cz/

## Spuštění
1. Nainstalujte JDK 21 a Maven.
2. V kořenovém adresáři spusťte:
   ```bash
   mvn spring-boot:run
   ```
3. Při prvním spuštění se stáhne CSV z ČSÚ a proběhne import do databáze. První start proto může trvat déle; internetové připojení je nutné.
4. Otevřete http://localhost:8080.

Importovaný CSV soubor je uložen v `data/res_data.csv`, lokální databáze v `data/resdb`. Tyto soubory nejsou součástí zdrojového ZIPu. Při dalších spuštěních aplikace již import neopakuje, pokud databáze obsahuje záznamy.

## Funkce
- Tabulka ekonomických subjektů z RES ČSÚ.
- Stránkování (25 záznamů na stránku), vyhledávání podle názvu nebo IČO v lokálním datasetu.
- Filtr pouze aktivních subjektů.
- Režim samostatného online vyhledávání přes ARES.
- Zobrazení obce, právní formy, data vzniku a kategorie počtu pracovníků.

## Poznámky k datům
RES je statistický registr ekonomických subjektů, nikoli výhradně seznam obchodních společností. Obsahuje právnické osoby, fyzické osoby podnikatele a další vymezené subjekty. „Pouze aktivní“ je v aplikaci filtrováno podle chybějícího data zániku (`DDATZAN`).

ČSÚ datové atributy zveřejňuje podle metodiky RES. Kategorie pracovníků není přesný počet zaměstnanců. Záznamy jsou platné k hodnotě `DATPLAT` ve snapshotu. Data mohou obsahovat prázdné údaje.

## API
- `GET /api/companies?query=&activeOnly=true&page=0&size=25` – stránkovaný seznam z RES
- `GET /api/companies/stats` – základní počty
- `GET /api/companies/{ico}` – detail z lokální RES databáze
- `GET /api/companies/ares/search?query=` – online ARES search

## Stav
Assessment prototyp. Pro produkční nasazení by bylo vhodné řešit automatickou aktualizaci snapshotu ČSÚ, atomický refresh databáze a testy importu/mapování.
