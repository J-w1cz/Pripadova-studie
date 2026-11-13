
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

    /**
     * Seznam subjektů z ČSÚ RES.
     * Filtruje podle obce a aktivního statusu.
     */
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
                res.find(normalizedLocation, activeOnly, page, size + 1);

        boolean hasNext = results.size() > size;

        List<CompanyDto> items = hasNext
                ? results.subList(0, size)
                : results;

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", page);
        response.put("size", size);
        response.put("hasNext", hasNext);
        response.put("location", normalizedLocation == null ? "" : normalizedLocation);
        response.put("activeOnly", activeOnly);

        return response;
    }

    /**
     * Detail subjektu z lokální databáze ČSÚ.
     */
    public CompanyDto detail(String ico) {
        validateIco(ico);
        return res.findByIco(ico);
    }

    /**
     * Vyhledávání přes ARES.
     */
    public Map<String, Object> aresSearch(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Vyhledávací dotaz nesmí být prázdný."
            );
        }

        validatePagination(page, size);

        List<CompanyDto> results =
                ares.search(query.trim(), page, size + 1);

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
