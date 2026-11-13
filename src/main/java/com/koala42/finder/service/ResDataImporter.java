package com.koala42.finder.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@Component
public class ResDataImporter implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ResDataImporter.class);
    private static final String CSV_URL = "https://opendata.csu.gov.cz/soubory/od/od_org03/res_data.csv";
    private final JdbcTemplate jdbc;

    public ResDataImporter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS companies (
              ico VARCHAR(8) PRIMARY KEY, name VARCHAR(1000), municipality VARCHAR(300),
              address VARCHAR(1000), legal_form VARCHAR(30), established VARCHAR(20),
              terminated VARCHAR(20), employee_category VARCHAR(30), nace VARCHAR(30),
              data_valid_as_of VARCHAR(20)
            )
            """);
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_companies_name ON companies(name)");
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM companies", Long.class);
        if (count != null && count > 0) {
            log.info("RES data already loaded: {} records.", count);
            return;
        }

        Path dataDir = Path.of("data");
        Files.createDirectories(dataDir);
        Path csvPath = dataDir.resolve("res_data.csv");
        if (!Files.exists(csvPath) || Files.size(csvPath) == 0) {
            log.info("Downloading current CSV snapshot from ČSÚ RES...");
            Path tempCsv = dataDir.resolve("res_data.csv.download");
            try (var in = URI.create(CSV_URL).toURL().openStream()) {
                Files.copy(in, tempCsv, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempCsv, csvPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Importing RES CSV into local H2 database. This first run may take a while.");
        long imported = 0;
        String sql = "INSERT INTO companies (ico,name,municipality,address,legal_form,established,terminated,employee_category,nace,data_valid_as_of) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            try (Reader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(csvPath), StandardCharsets.UTF_8));
                 CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
                List<Object[]> batch = new ArrayList<>(500);
                for (CSVRecord record : parser) {
                    batch.add(new Object[]{
                        val(record, "ICO"), val(record, "FIRMA"), val(record, "OBEC_TEXT"),
                        buildAddress(record), first(val(record, "ROSFORMA"), val(record, "FORMA")),
                        val(record, "DDATVZN"), val(record, "DDATZAN"), val(record, "KATPO"),
                        first(val(record, "NACE2025"), val(record, "NACE")), val(record, "DATPLAT")
                    });
                    if (batch.size() == 500) {
                        insertBatch(sql, batch); imported += batch.size(); batch.clear();
                    }
                }
                if (!batch.isEmpty()) { insertBatch(sql, batch); imported += batch.size(); }
            }
        } catch (Exception ex) {
            jdbc.execute("DELETE FROM companies");
            throw ex;
        }
        log.info("RES import finished. {} records loaded.", imported);
    }

    private void insertBatch(String sql, List<Object[]> batch) {
        jdbc.batchUpdate(sql, batch, batch.size(), (PreparedStatement ps, Object[] row) -> {
            for (int i = 0; i < row.length; i++) ps.setString(i + 1, (String) row[i]);
        });
    }

    private String val(CSVRecord row, String key) {
        if (!row.isMapped(key)) return null;
        String value = row.get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String first(String a, String b) { return a != null ? a : b; }

    private String buildAddress(CSVRecord r) {
        String fromParts = join(val(r, "ULICE_TEXT"), val(r, "CDOM"), val(r, "COR"),
                val(r, "PSC"), val(r, "OBEC_TEXT"));
        return fromParts.isBlank() ? val(r, "TEXTADR") : fromParts;
    }

    private String join(String... parts) {
        return java.util.Arrays.stream(parts).filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
