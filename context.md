# KONTEXT K PRIPADOVE STUDII

# Celou pripadovou studii zpracovavam zpusobem, kterym bych postupoval, pokud by klient toto reseni pozadoval primo ode me, tudiz bez toho, abych se ridil zabehlymi procesy konkretni spolecnosti.

## A/ Preprodani jineho hotoveho reseni

Prvnim krokem je nepochybne zjistit, zdali danou sluzbu jiz nekdo nenabizi. Jelikoz nemam informaci, k cemu presne klient bude aplikaci vyuzivat, mohu postupovat pouze podle podobnosti reseni ku zadani. Pokud klient chce nasi aplikaci vyuzivat pro ucely obchodu, tedy napr. volat a psat leadum, urcite se vyplati koupit hotove reseni a nasledne ho klientovi preprodat. Vlastni vyvoj ma cenu pouze v nekterych pripadech, napr. pokud klient potrebuje aplikaci napojit na specificky interni proces nebo pokud vyslovne chce vybudovat vlastni aplikaci obdobneho typu. Jinak se vsak kompletni vyvoj pravdepodobne nezaplati. Podle meho pruzkumu zadani nejlepe splnuje [https://www.merk.cz/](https://www.merk.cz/), kde se cena vypocitava individualne, ale dle internetovych diskuzi a clanku se cena pohybuje od nekolika stovek po nizsi tisice mesicne. Drazsi variantou s pokrytim mimo Ceskou republiku je napriklad [https://www.bizmachine.com/prospector](https://www.bizmachine.com/prospector), nabizi vsak vice, nez co potrebujeme pro ucely nasi aplikace.

[https://www.bizmachine.com/blog/cognism-alternativy](https://www.bizmachine.com/blog/cognism-alternativy)

[https://webtrh.cz/diskuse/aplikace-merk-cz/](https://webtrh.cz/diskuse/aplikace-merk-cz/)

## B/ Aplikace

Vytvoril jsem webovy prototyp v Jave 21 a Spring Bootu. Lokalni seznam subjektu vychazi z otevreneho CSV Registru ekonomickych subjektu CSU, ktere se pri prvnim spusteni stahne a importuje do souborove databaze H2. Lokalne lze filtrovat podle obce a aktivniho stavu. Vyhledavani nazvu a ICO je oddelene a pouziva online API ARES. Kliknuti na radek otevre zaznam na webu Justice.

### Rozhodnuti a kompromisy

- **CSU RES jako zaklad:** je to verejny, strukturovany zdroj pokryvajici velke mnozstvi subjektu a umoznuje ukazat prochazeni a filtrovani lokalniho datasetu. RES ale neni cisty seznam obchodnich spolecnosti a jeho pole nepokryvaji vsechny obchodni potreby.
- **ARES pro hledani nazvu a ICO:** misto stahovani a spravy dalsiho zdroje vyuzivam online API. Tim se zmensil rozsah prototypu, ale tato cast zavisi na dostupnosti sluzby a jeji podobe. Nejde o hledani v lokalni databazi, coz je v README vyslovne uvedeno.
- **H2 a jednorazovy import:** souborova databaze a import pri prvnim startu umoznily rychle pripravit funkcni prototyp bez infrastruktury databazoveho serveru. Znamena to vsak pomaly prvni start a nutnost internetu.
- **Obrat a kontaktni udaje jsem neimplementoval:** v ramci priblizne 4-5 hodin jsem uprednostnil funkcni datovy tok, import, filtrovani, strankovani a zakladni UI. Obrat, vlastniky, management, telefony a e-maily by bylo nutne resit samostatnym pruzkumem zdroju, jejich pokryti, licenci a kvality. Verejna dostupnost udaje sama o sobe neznamena, ze je vhodne jej bez dalsiho agregovat a pouzivat pro obchodni oslovovani. Justice informace o obratu spolecnosti v ramci ucetnich zavek obsahuje, ale bylo by treba pripravit parser.
- **Zakladni detail misto uplneho profilu:** API vraci udaje dostupne v lokalnim RES, ale aplikace zatim nema detailni obrazovku s napojenymi registry. Kliknuti na radek vede na Justici.
- **Data v Git historii:** CSV a databazove soubory se dostaly do historie repozitare. Protoze jsou odvoditelne z verejneho zdroje a databazi lze znovu vytvorit importem, nemely by byt soucasti dlouhodobe historie. Samotne smazani souboru v novem commitu historii nezmensi; je potreba historii prepsat a binarni soubory vycistit ze vsech revizi. Tento krok je popsán take v README.

## C/ Mozne rozsireni a nabidka klientovi

Dalsi postup bych navrhl podle toho, zda klient chce seznam firem pro interni analyzu, nebo nastroj pro obchodni akvizici. Nejprve bych s klientem overil prioritni segmenty, pozadovane regiony, minimalni obrat, vyznam pojmu „spolecnost“ a to, zda ocekava kontakty na pravnicke osoby nebo na konkretni lidi.

Mozne rozsireni prototypu:

1. **Lepsi filtrovani a detail:** pravni forma, obor/NACE, datum vzniku, velikost podle dostupne kategorie zamestnancu, presnejsi prace s lokalitou a uplna detailni stranka se zdroji a datem aktualizace.
2. **Obrat a ekonomicke ukazatele:** vyhledat vhodny verejny ci licencovany zdroj, overit pokryti a casovou aktualnost a zobrazovat puvod hodnoty. Pokud nelze spolehlivy obrat ziskat pro dostatecne mnozstvi subjektu, nabidnout klientovi alternativni filtr podle dostupneho ukazatele, napr. kategorie zamestnancu.
3. **Vlastnici a management:** propojit subjekt s prislusnymi verejnymi rejstriky, zobrazit role a datum, ke kteremu udaj plati, a resit zmeny i rozpory mezi zdroji.
4. **Telefon a e-mail:** doplnit jen z overenych zdroju a vzdy uvadet puvod udaje. Pred pouzitim pro obchodni kontaktovani posoudit pravni titul, pravidla ochrany osobnich udaju a podminky zdroju.
5. **Provozni pripravenost:** automaticka aktualizace, bezpecny atomicky import, databaze vhodna pro cloud, logovani, monitoring, testy importu a mapovani a nasazeni s navodem pro spravu.
6. **Prace s leady:** export vysledku, ukladani vyberu, poznamky a pripadne napojeni na CRM. Tyto funkce bych navrhoval az po potvrzeni skutecneho workflow klienta.

Za nejrozumnejsi dalsi krok povazuji kratke overeni existujicich komercnich nastroju proti potrebam klienta. Pokud nektery z nich pokryva pozadovane udaje a exporty, muze byt nakup levnejsi a rychlejsi. Vlastni aplikaci bych rozsiroval tam, kde je prokazatelna mezera nebo potrebna integrace, kterou hotovy produkt nepokryva.