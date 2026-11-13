package com.koala42.finder.controller;

import com.koala42.finder.model.CompanyDto;
import com.koala42.finder.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyService service;
    public CompanyController(CompanyService service) { this.service = service; }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "") String query,
                                    @RequestParam(defaultValue = "true") boolean activeOnly,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "25") int size) {
        return service.list(query, activeOnly, page, size);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() { return service.stats(); }

    @GetMapping("/{ico}")
    public ResponseEntity<CompanyDto> detail(@PathVariable String ico) {
        CompanyDto company = service.detail(ico);
        return company == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(company);
    }

    @GetMapping("/ares/search")
    public List<CompanyDto> aresSearch(@RequestParam String query) {
        return service.aresSearch(query);
    }
}
