
package com.koala42.finder.service;

import com.koala42.finder.model.CompanyDto;
import com.koala42.finder.provider.AresApiProvider;
import com.koala42.finder.repository.ResRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompanyService {

    private final ResRepository res;
    private final AresApiProvider ares;

    public CompanyService(ResRepository res, AresApiProvider ares) {
        this.res = res;
        this.ares = ares;
    }

    public Map<String, Object> list(
            String location,
            boolean activeOnly,
            int page,
            int size
    ) {
        validatePagination(page, size);

        String normalizedLocation =
                location == null || location.isBlank()
                        ? null
                        : location.trim();

        List<CompanyDto> results =
                res.find(normalizedLocation, activeOnly, page, size);

        boolean hasNext = results.size() > size;

        List<CompanyDto> items = hasNext
                ? results.subList(0, size)
                : results;

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", page);
        response.put("size", size);
        response.put("hasNext", hasNext);
        response.put("location",
                normalizedLocation == null ? "" : normalizedLocation);
        response.put("activeOnly", activeOnly);

        return response;
    }

    public Map<String, Object> search(
            String type,
            String query,
            boolean activeOnly,
            int page,
            int size
    ) {
        validatePagination(page, size);

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Typ vyhledávání musí být zadán."
            );
        }

        String normalizedType = type.trim().toLowerCase();
        String normalizedQuery =
                query == null ? "" : query.trim();

        List<CompanyDto> results;

        switch (normalizedType) {
            case "name":
                if (normalizedQuery.isBlank()) {
                    throw new IllegalArgumentException(
                            "Zadejte název společnosti."
                    );
                }

                results = ares.search(
                        normalizedQuery, page, size, false
                );
                break;

            case "ico":
                validateIco(normalizedQuery);

                results = ares.search(
                        normalizedQuery, page, size, true
                );
                break;

            case "municipality":
                results = res.find(
                        normalizedQuery.isBlank() ? null : normalizedQuery,
                        activeOnly,
                        page,
                        size
                );
                break;

            default:
                throw new IllegalArgumentException(
                        "Neznámý typ vyhledávání: " + normalizedType
                );
        }

        boolean hasNext = results.size() > size;

        List<CompanyDto> items = hasNext
                ? results.subList(0, size)
                : results;

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", page);
        response.put("size", size);
        response.put("hasNext", hasNext);
        response.put("query", normalizedQuery);
        response.put("type", normalizedType);

        return response;
    }

    public CompanyDto detail(String ico) {
        validateIco(ico);
        return res.findByIco(ico);
    }

    public Map<String, Object> aresSearch(
            String query,
            int page,
            int size
    ) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Vyhledávací dotaz nesmí být prázdný."
            );
        }

        validatePagination(page, size);

        List<CompanyDto> results =
                ares.search(query.trim(), page, size);

        boolean hasNext = results.size() > size;

        List<CompanyDto> items = hasNext
                ? results.subList(0, size)
                : results;

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", page);
        response.put("size", size);
        response.put("hasNext", hasNext);
        response.put("query", query.trim());

        return response;
    }

    public Map<String, Object> stats() {
        return res.stats();
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Stránka nesmí být záporná."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Velikost stránky musí být 1 až 100."
            );
        }
    }

    private void validateIco(String ico) {
        if (ico == null || !ico.matches("\\d{8}")) {
            throw new IllegalArgumentException(
                    "IČO musí obsahovat přesně 8 číslic."
            );
        }
    }
}
