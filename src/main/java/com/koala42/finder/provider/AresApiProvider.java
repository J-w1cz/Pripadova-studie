package com.koala42.finder.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koala42.finder.model.CompanyDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Jediná třída, která komunikuje přímo s ARES.
 * Provider odděluje externí JSON formát od našeho DTO.
 */
@Component
public class AresApiProvider {
    private static final String BASE_URL =
            "https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/v3";
    private final RestClient client = RestClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("Accept", "application/json")
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<CompanyDto> search(String query) {
        // ARES očekává POST s JSON tělem; obchodníJméno slouží pro textové hledání.
        JsonNode response = client.post()
                .uri("/ekonomicke-subjekty/vyhledat")
                .body(java.util.Map.of("obchodniJmeno", query))
                .retrieve()
                .body(JsonNode.class);

        List<CompanyDto> result = new ArrayList<>();
        if (response == null) return result;

        JsonNode subjects = response.path("ekonomickeSubjekty");
        if (!subjects.isArray()) subjects = response.path("seznamEkonomickychSubjektu");
        if (!subjects.isArray()) return result;

        for (JsonNode subject : subjects) {
            result.add(map(subject));
        }
        return result;
    }

    public CompanyDto detail(String ico) {
        JsonNode subject = client.get()
                .uri("/ekonomicke-subjekty/{ico}", ico)
                .retrieve()
                .body(JsonNode.class);
        return subject == null ? null : map(subject);
    }

    private CompanyDto map(JsonNode node) {
        String ico = text(node, "ico");
        String name = text(node, "obchodniJmeno");
        if (name == null) name = text(node, "nazev");
        JsonNode addressNode = node.path("sidlo");
        String address = text(addressNode, "textovaAdresa");
        if (address == null) {
            address = join(text(addressNode, "nazevUlice"),
                    text(addressNode, "cisloDomovni"), text(addressNode, "nazevObce"));
        }
        String legalForm = text(node, "pravniForma");
        String established = text(node, "datumVzniku");
        return new CompanyDto(ico, name, text(addressNode, "nazevObce"), address, legalForm,
                established, text(node, "datumZaniku"), null, null, null, "ARES API");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) return null;
        if (value.isValueNode()) return value.asText();
        // Některé ARES hodnoty mohou být objekty s kódem a názvem.
        String name = value.path("nazev").asText(null);
        return name != null ? name : value.path("kod").asText(null);
    }

    private String join(String... values) {
        return java.util.Arrays.stream(values)
                .filter(v -> v != null && !v.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
