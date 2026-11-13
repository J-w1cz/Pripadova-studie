package com.koala42.finder.service;

import com.koala42.finder.model.CompanyDto;
import com.koala42.finder.provider.AresApiProvider;
import com.koala42.finder.repository.ResRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CompanyService {
    private final ResRepository res;
    private final AresApiProvider ares;

    public CompanyService(ResRepository res, AresApiProvider ares) {
        this.res = res; this.ares = ares;
    }

    public Map<String, Object> list(String query, boolean activeOnly, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("Stránka nesmí být záporná.");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Velikost stránky musí být 1 až 100.");
        String q = query == null ? "" : query.trim();
        long total = res.count(q, activeOnly);
        return Map.of("items", res.find(q, activeOnly, page, size), "total", total,
                "page", page, "size", size, "pages", (int)Math.ceil((double) total / size));
    }

    public CompanyDto detail(String ico) {
        if (ico == null || !ico.matches("\\d{8}")) throw new IllegalArgumentException("IČO musí obsahovat přesně 8 číslic.");
        return res.findByIco(ico);
    }

    public List<CompanyDto> aresSearch(String query) {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Vyhledávací dotaz nesmí být prázdný.");
        return ares.search(query.trim());
    }

    public Map<String, Object> stats() { return res.stats(); }
}
