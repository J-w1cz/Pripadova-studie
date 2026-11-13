
package com.koala42.finder.controller;

import com.koala42.finder.model.CompanyDto;
import com.koala42.finder.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    /**
     * ČSÚ RES - seznam subjektů.
     */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return service.list(location, activeOnly, page, size);
    }

    /**
     * Statistiky databáze.
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return service.stats();
    }

    /**
     * Detail subjektu podle IČO.
     */
    @GetMapping("/{ico}")
    public ResponseEntity<CompanyDto> detail(@PathVariable String ico) {
        CompanyDto company = service.detail(ico);

        return company == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(company);
    }

    /**
     * ARES - externí vyhledávání.
     */
    @GetMapping("/ares/search")
    public Map<String, Object> aresSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return service.aresSearch(query, page, size);
    }
}
