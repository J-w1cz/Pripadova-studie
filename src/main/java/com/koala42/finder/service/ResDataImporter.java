
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
import java.util.Locale;

@Component
public class ResDataImporter implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(ResDataImporter.class);

    private static final String CSV_URL =
            "https://opendata.csu.gov.cz/soubory/od/od_org03/res_data.csv";

    private static final int BATCH_SIZE = 2000;

    private final JdbcTemplate jdbc;

    public ResDataImporter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        createTables();
        // Migrace existující databáze.
        migrateDatabase();
        

        Boolean completed = jdbc.queryForObject(
                "SELECT COUNT(*) > 0 FROM app_metadata "
                        + "WHERE metadata_key = 'res_import_complete'",
                Boolean.class
        );

        if (Boolean.TRUE.equals(completed)) {
            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM companies",
                    Long.class
            );

            log.info("RES data already loaded: {} records.", count);

            createIndexes();

            return;
        }

        // Odstranění případných dat z přerušeného importu.
        jdbc.update("DELETE FROM companies");

        Path dataDir = Path.of("data");
        Files.createDirectories(dataDir);

        Path csvPath = dataDir.resolve("res_data.csv");

        if (!Files.exists(csvPath) || Files.size(csvPath) == 0) {
            log.info("Downloading current CSV snapshot from ČSÚ...");

            Path tempCsv = dataDir.resolve("res_data.csv.download");

            try (var in = URI.create(CSV_URL).toURL().openStream()) {
                Files.copy(
                        in,
                        tempCsv,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }

            Files.move(
                    tempCsv,
                    csvPath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
        }

        log.info("Starting RES import...");

        long imported = importCsv(csvPath);

        createIndexes();

        jdbc.update(
                "MERGE INTO app_metadata (metadata_key, metadata_value) "
                        + "KEY(metadata_key) VALUES (?, ?)",
                "res_import_complete",
                "true"
        );

        log.info("RES import finished. {} records loaded.", imported);
    }

    private void createTables() {

        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS companies (
                    ico VARCHAR(8) PRIMARY KEY,
                    name VARCHAR(1000),
                    municipality VARCHAR(300),
                    municipality_key VARCHAR(300),
                    address VARCHAR(1000),
                    legal_form VARCHAR(30),
                    established VARCHAR(20),
                    terminated VARCHAR(20),
                    active BOOLEAN,
                    employee_category VARCHAR(30),
                    nace VARCHAR(30),
                    data_valid_as_of VARCHAR(20)
                )
                """);

        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS app_metadata (
                    metadata_key VARCHAR(100) PRIMARY KEY,
                    metadata_value VARCHAR(500)
                )
                """);
    }

    
private void migrateDatabase() {

    Boolean migrated = jdbc.queryForObject(
            """
            SELECT COUNT(*) > 0
            FROM app_metadata
            WHERE metadata_key = 'companies_schema_version'
              AND metadata_value = '2'
            """,
            Boolean.class
    );

    if (Boolean.TRUE.equals(migrated)) {
        log.info("Database schema is already up to date.");
        return;
    }

    log.info("Starting database migration to schema version 2...");

    jdbc.execute("""
            ALTER TABLE companies
            ADD COLUMN IF NOT EXISTS municipality_key VARCHAR(300)
            """);

    jdbc.execute("""
            ALTER TABLE companies
            ADD COLUMN IF NOT EXISTS active BOOLEAN
            """);

    log.info("Updating active status...");

    jdbc.update("""
            UPDATE companies
            SET active = CASE
                    WHEN terminated IS NULL OR terminated = '' THEN TRUE
                    ELSE FALSE
                END
            WHERE active IS NULL
            """);

    log.info("Updating municipality keys...");

    jdbc.update("""
            UPDATE companies
            SET municipality_key = UPPER(municipality)
            WHERE municipality_key IS NULL
              AND municipality IS NOT NULL
            """);

    // Verzi uložíme až po úspěšném dokončení migrace.
    jdbc.update("""
            MERGE INTO app_metadata (metadata_key, metadata_value)
            KEY(metadata_key) VALUES (?, ?)
            """,
            "companies_schema_version",
            "2"
        );

        log.info("Database migration to schema version 2 finished.");
        }


    private void createIndexes() {

        // Odstranění starých indexů, které nahrazujeme.
        jdbc.execute(
                "DROP INDEX IF EXISTS idx_companies_name"
        );

        jdbc.execute(
                "DROP INDEX IF EXISTS idx_companies_municipality"
        );

        // Index pro načítání aktivních společností.
        jdbc.execute("""
                CREATE INDEX IF NOT EXISTS idx_companies_active_name
                ON companies(active, name, ico)
                """);

        // Index pro vyhledávání podle obce.
        jdbc.execute("""
                CREATE INDEX IF NOT EXISTS idx_companies_municipality_active
                ON companies(active, municipality_key, name, ico)
                """);
    }

    private long importCsv(Path csvPath) throws Exception {

        long imported = 0;

        String sql = """
                INSERT INTO companies (
                    ico, name, municipality, municipality_key, address,
                    legal_form, established, terminated, active,
                    employee_category, nace, data_valid_as_of
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (
                Reader reader = new BufferedReader(
                        new InputStreamReader(
                                Files.newInputStream(csvPath),
                                StandardCharsets.UTF_8
                        )
                );

                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .get()
                        .parse(reader)
        ) {

            List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

            for (CSVRecord record : parser) {

                String municipality = val(record, "OBEC_TEXT");
                String terminated = val(record, "DDATZAN");

                batch.add(new Object[]{
                        val(record, "ICO"),
                        val(record, "FIRMA"),
                        municipality,
                        municipality == null
                                ? null
                                : municipality.toUpperCase(Locale.ROOT),
                        buildAddress(record),
                        first(
                                val(record, "ROSFORMA"),
                                val(record, "FORMA")
                        ),
                        val(record, "DDATVZN"),
                        terminated,
                        terminated == null,
                        val(record, "KATPO"),
                        first(
                                val(record, "NACE2025"),
                                val(record, "NACE")
                        ),
                        val(record, "DATPLAT")
                });

                if (batch.size() >= BATCH_SIZE) {
                    insertBatch(sql, batch);
                    imported += batch.size();
                    batch.clear();

                    if (imported % 100000 == 0) {
                        log.info("Imported {} records...", imported);
                    }
                }
            }

            if (!batch.isEmpty()) {
                insertBatch(sql, batch);
                imported += batch.size();
            }

        } catch (Exception ex) {
            jdbc.update("DELETE FROM companies");
            throw ex;
        }

        return imported;
    }

    private void insertBatch(String sql, List<Object[]> batch) {

        jdbc.batchUpdate(
                sql,
                batch,
                batch.size(),
                (PreparedStatement ps, Object[] row) -> {

                    for (int i = 0; i < row.length; i++) {

                        if (row[i] instanceof Boolean) {
                            ps.setBoolean(i + 1, (Boolean) row[i]);
                        } else {
                            ps.setString(i + 1, (String) row[i]);
                        }
                    }
                }
        );
    }

    private String val(CSVRecord row, String key) {

        if (!row.isMapped(key)) {
            return null;
        }

        String value = row.get(key);

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private String first(String a, String b) {
        return a != null ? a : b;
    }

    private String buildAddress(CSVRecord r) {

        String fromParts = join(
                val(r, "ULICE_TEXT"),
                val(r, "CDOM"),
                val(r, "COR"),
                val(r, "PSC"),
                val(r, "OBEC_TEXT")
        );

        return fromParts.isBlank()
                ? val(r, "TEXTADR")
                : fromParts;
    }

    private String join(String... parts) {
        return java.util.Arrays.stream(parts)
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
