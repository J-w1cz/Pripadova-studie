
package com.koala42.finder.repository;

import com.koala42.finder.model.CompanyDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ResRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<CompanyDto> MAPPER = (rs, rowNum) ->
            new CompanyDto(
                    rs.getString("ico"),
                    rs.getString("name"),
                    rs.getString("municipality"),
                    rs.getString("address"),
                    rs.getString("legal_form"),
                    rs.getString("established"),
                    rs.getString("terminated"),
                    rs.getString("employee_category"),
                    rs.getString("nace"),
                    rs.getString("data_valid_as_of"),
                    "ČSÚ RES"
            );

    public ResRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Načte stránku subjektů.
     * Volající předává size + 1, aby bylo možné zjistit hasNext.
     */
    public List<CompanyDto> find(
            String location,
            boolean activeOnly,
            int page,
            int size
    ) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM companies WHERE 1=1 "
        );

        List<Object> parameters = new java.util.ArrayList<>();

        if (activeOnly) {
            sql.append("AND (terminated IS NULL OR terminated = '') ");
        }

        if (location != null && !location.isBlank()) {
            sql.append("AND LOWER(municipality) LIKE LOWER(?) ");
            parameters.add(location.trim() + "%");
        }

        sql.append("ORDER BY name, ico LIMIT ? OFFSET ?");

        parameters.add(size);
        parameters.add((long) page * size);

        return jdbc.query(
                sql.toString(),
                MAPPER,
                parameters.toArray()
        );
    }

    public CompanyDto findByIco(String ico) {
        List<CompanyDto> found = jdbc.query(
                "SELECT * FROM companies WHERE ico = ?",
                MAPPER,
                ico
        );

        return found.isEmpty() ? null : found.get(0);
    }

    public Map<String, Object> stats() {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM companies",
                Long.class
        );

        Long active = jdbc.queryForObject(
                "SELECT COUNT(*) FROM companies "
                        + "WHERE terminated IS NULL OR terminated = ''",
                Long.class
        );

        String asOf = jdbc.queryForObject(
                "SELECT MAX(data_valid_as_of) FROM companies",
                String.class
        );

        return Map.of(
                "total", total == null ? 0 : total,
                "active", active == null ? 0 : active,
                "dataValidAsOf", asOf == null ? "" : asOf
        );
    }
}
