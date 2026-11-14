
package com.koala42.finder.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koala42.finder.model.CompanyDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AresApiProvider {

    private static final String BASE_URL =
        "https://ares.gov.cz/ekonomicke-subjekty-v-be/rest";

    private final RestClient client = RestClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("Accept", "application/json")
            .defaultHeader("Content-Type", "application/json")
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Vyhledávání v ARES s podporou stránkování.
     */
    
    
public List<CompanyDto> search(
        String query,
        int page,
        int size
) {
    boolean byIco = query != null && query.matches("\\d{8}");
    return search(query, page, size, byIco);
}

public List<CompanyDto> search(
        String query,
        int page,
        int size,
        boolean byIco
) {
    if (query == null || query.isBlank()) {
        return List.of();
    }

    if (page < 0 || size < 1 || size > 100) {
        throw new IllegalArgumentException("Neplatné stránkování.");
    }

    String normalizedQuery = query.trim();

    Map<String, Object> request;

    if (byIco) {
        if (!normalizedQuery.matches("\\d{8}")) {
            throw new IllegalArgumentException("Neplatné IČO.");
        }

        request = Map.of(
                "ico", List.of(normalizedQuery),
                "start", page * size,
                "pocet", size + 1
        );
    } else {
        request = Map.of(
                "obchodniJmeno", normalizedQuery,
                "start", page * size,
                "pocet", size + 1
        );
    }

    try {
        JsonNode response = client.post()
                .uri("/ekonomicke-subjekty/vyhledat")
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        List<CompanyDto> result = new ArrayList<>();

        if (response == null) {
            return result;
        }

        JsonNode subjects = response.path("ekonomickeSubjekty");

        if (!subjects.isArray()) {
            subjects = response.path("seznamEkonomickychSubjektu");
        }

        if (!subjects.isArray()) {
            return result;
        }

        for (JsonNode subject : subjects) {
            CompanyDto company = map(subject);

            if (company != null) {
                result.add(company);
            }
        }

        return result;

    } catch (RestClientResponseException e) {
        throw new RuntimeException(
                "ARES HTTP " + e.getStatusCode()
                        + ": " + e.getResponseBodyAsString(),
                e
        );
    } catch (Exception e) {
        throw new RuntimeException(
                "Chyba při komunikaci s ARES: " + e.getMessage(),
                e
        );
    }
}



    /**
     * Zpětná kompatibilita se starším voláním.
     */
    public List<CompanyDto> search(String query) {
        return search(query, 0, 25);
    }

    /**
     * Detail subjektu podle IČO.
     */
    public CompanyDto detail(String ico) {

        if (ico == null || !ico.matches("\\d{8}")) {
            throw new IllegalArgumentException("Neplatné IČO.");
        }

        try {
            JsonNode subject = client.get()
                    .uri("/ekonomicke-subjekty/{ico}", ico)
                    .retrieve()
                    .body(JsonNode.class);

            return subject == null ? null : map(subject);

        } catch (RestClientResponseException e) {
            throw new RuntimeException(
                    "ARES vrátil HTTP " + e.getStatusCode()
                            + ". Odpověď: " + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private CompanyDto map(JsonNode node) {

        String ico = text(node, "ico");

        String name = text(node, "obchodniJmeno");

        if (name == null) {
            name = text(node, "nazev");
        }

        JsonNode addressNode = node.path("sidlo");

        String municipality = text(addressNode, "nazevObce");

        String address = text(addressNode, "textovaAdresa");

        if (address == null) {
            address = join(
                    text(addressNode, "nazevUlice"),
                    text(addressNode, "cisloDomovni"),
                    text(addressNode, "cisloOrientacni"),
                    municipality,
                    text(addressNode, "psc")
            );
        }

        String legalForm = text(node, "pravniForma");

        String established = text(node, "datumVzniku");

        String terminated = text(node, "datumZaniku");

        if (ico == null || name == null) {
            return null;
        }

        return new CompanyDto(
                ico,
                name,
                municipality,
                address,
                legalForm,
                established,
                terminated,
                null,
                null,
                null,
                "ARES API"
        );
    }

    private String text(JsonNode node, String field) {

        JsonNode value = node.path(field);

        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        if (value.isValueNode()) {
            return value.asText();
        }

        String name = value.path("nazev").asText(null);

        if (name != null) {
            return name;
        }

        return value.path("kod").asText(null);
    }

    private String join(String... values) {
        return java.util.Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
