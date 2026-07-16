package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * WP-S2-08: deterministic fixture families for all four templates (TEST-02)
 * plus the NFR-03 benchmark (100,000 rows imported in under 3 minutes).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LargeCsvImportBenchmarkIT extends PostgreSqlTestContainerSupport {

    static final String GENERATOR_VERSION = "1";
    static final int BENCHMARK_ROWS = 100_000;
    static final long BUDGET_MILLIS = 180_000;

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";
    private static final String FIXTURE_DIR = "/fixtures/imports/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;

    @BeforeEach
    void ensureMasterData() throws Exception {
        adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        if (count("factory where code = 'BENCH-F'") > 0) {
            return;
        }
        String factoryId = create("/factories", """
                {"code":"BENCH-F","name":"Benchmark Factory"}
                """);
        String lineId = create("/production-lines", """
                {"factoryId":"%s","code":"BENCH-L","name":"Benchmark Line"}
                """.formatted(factoryId));
        create("/machines", """
                {"lineId":"%s","code":"BENCH-M","name":"Benchmark Machine"}
                """.formatted(lineId));
        create("/products", """
                {"code":"BENCH-P","name":"Benchmark Product","family":"Bench"}
                """);
        create("/shifts", """
                {"code":"BENCH-S","name":"Benchmark Shift","startTime":"06:00:00","endTime":"14:00:00","plannedMinutes":480}
                """);
        create("/downtime-reasons", """
                {"code":"BENCH-R","name":"Benchmark Reason"}
                """);
        create("/scrap-categories", """
                {"code":"BENCH-C","name":"Benchmark Category"}
                """);
    }

    @Test
    void productionFixtureFamilyMatchesExpectedCounts() throws Exception {
        String validJobId = upload("/import-jobs/production-records", fixture("production-records.valid.csv"));
        assertCommitted(validJobId, 5);
        assertThat(count("production_record where import_job_id = '%s'".formatted(validJobId))).isEqualTo(5);

        String invalidJobId = upload("/import-jobs/production-records", fixture("production-records.invalid.csv"));
        assertFailed(invalidJobId, 3);
        assertThat(count("production_record where import_job_id = '%s'".formatted(invalidJobId))).isZero();
    }

    @Test
    void energyFixtureFamilyMatchesExpectedCounts() throws Exception {
        String validJobId = upload("/import-jobs/energy-measurements", fixture("energy-measurements.valid.csv"));
        assertCommitted(validJobId, 5);
        assertThat(count("energy_measurement where import_job_id = '%s'".formatted(validJobId))).isEqualTo(5);

        String invalidJobId = upload("/import-jobs/energy-measurements", fixture("energy-measurements.invalid.csv"));
        assertFailed(invalidJobId, 3);
        assertThat(count("energy_measurement where import_job_id = '%s'".formatted(invalidJobId))).isZero();
    }

    @Test
    void downtimeFixtureFamilyMatchesExpectedCounts() throws Exception {
        String validJobId = upload("/import-jobs/downtime-records", fixture("downtime-records.valid.csv"));
        assertCommitted(validJobId, 5);
        assertThat(count("downtime_record where import_job_id = '%s'".formatted(validJobId))).isEqualTo(5);

        String invalidJobId = upload("/import-jobs/downtime-records", fixture("downtime-records.invalid.csv"));
        assertFailed(invalidJobId, 3);
        assertThat(count("downtime_record where import_job_id = '%s'".formatted(invalidJobId))).isZero();
    }

    @Test
    void scrapFixtureFamilyMatchesExpectedCounts() throws Exception {
        String validJobId = upload("/import-jobs/scrap-records", fixture("scrap-records.valid.csv"));
        assertCommitted(validJobId, 5);
        assertThat(count("scrap_record where import_job_id = '%s'".formatted(validJobId))).isEqualTo(5);

        String invalidJobId = upload("/import-jobs/scrap-records", fixture("scrap-records.invalid.csv"));
        assertFailed(invalidJobId, 3);
        assertThat(count("scrap_record where import_job_id = '%s'".formatted(invalidJobId))).isZero();
    }

    @Test
    void hundredThousandRowProductionImportStaysWithinNfr03Budget() throws Exception {
        byte[] csv = generateProductionCsv(BENCHMARK_ROWS);

        long startedAt = System.nanoTime();
        String jobId = upload("/import-jobs/production-records",
                new MockMultipartFile("file", "benchmark-100k.csv", "text/csv", csv));
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        assertCommitted(jobId, BENCHMARK_ROWS);
        assertThat(count("production_record where import_job_id = '%s'".formatted(jobId))).isEqualTo(BENCHMARK_ROWS);
        assertThat(elapsedMillis).isLessThan(BUDGET_MILLIS);

        recordEvidence(csv.length, elapsedMillis);
    }

    private static byte[] generateProductionCsv(int rows) {
        StringBuilder builder = new StringBuilder(rows * 110);
        builder.append("period_start,period_end,factory_code,line_code,machine_code,product_code,shift_code,units_produced,batch_code\n");
        Instant base = Instant.parse("2030-01-01T00:00:00Z");
        for (int index = 0; index < rows; index++) {
            Instant start = base.plusSeconds(3600L * index);
            Instant end = start.plusSeconds(3600L);
            builder.append(start).append(',').append(end)
                    .append(",BENCH-F,BENCH-L,BENCH-M,BENCH-P,BENCH-S,100,B-")
                    .append(index)
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void recordEvidence(int fileSizeBytes, long elapsedMillis) throws IOException {
        long rowsPerSecond = elapsedMillis == 0 ? BENCHMARK_ROWS : BENCHMARK_ROWS * 1000L / elapsedMillis;
        String evidence = """
                WP-S2-08 import benchmark (NFR-03: 100k rows < 3 minutes)
                generatorVersion=%s
                rows=%d
                fileSizeBytes=%d
                elapsedMillis=%d
                rowsPerSecond=%d
                budgetMillis=%d
                javaVersion=%s
                os=%s
                recordedAt=%s
                """.formatted(
                GENERATOR_VERSION,
                BENCHMARK_ROWS,
                fileSizeBytes,
                elapsedMillis,
                rowsPerSecond,
                BUDGET_MILLIS,
                System.getProperty("java.version"),
                System.getProperty("os.name") + " " + System.getProperty("os.version"),
                Instant.now());
        Path target = Path.of("target", "benchmark", "wp-s2-08-import-100k.txt");
        Files.createDirectories(target.getParent());
        Files.writeString(target, evidence, StandardCharsets.UTF_8);
        System.out.println(evidence);
    }

    private String upload(String path, MockMultipartFile file) throws Exception {
        MvcResult result = mockMvc.perform(multipart(path)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.jobId");
    }

    private void assertCommitted(String jobId, int expectedValidRows) {
        assertThat(jobStatus(jobId)).isEqualTo("COMMITTED");
        assertThat(jobColumn(jobId, "valid_rows")).isEqualTo(expectedValidRows);
        assertThat(jobColumn(jobId, "error_count")).isZero();
    }

    private void assertFailed(String jobId, int expectedErrorCount) {
        assertThat(jobStatus(jobId)).isEqualTo("FAILED");
        assertThat(jobColumn(jobId, "error_count")).isEqualTo(expectedErrorCount);
    }

    private MockMultipartFile fixture(String name) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(FIXTURE_DIR + name)) {
            assertThat(stream).as("fixture " + name).isNotNull();
            return new MockMultipartFile("file", name, "text/csv", stream.readAllBytes());
        }
    }

    private String create(String path, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String jobStatus(String jobId) {
        return jdbcTemplate.queryForObject("select status from import_job where id = ?", String.class, UUID.fromString(jobId));
    }

    private long jobColumn(String jobId, String column) {
        Long value = jdbcTemplate.queryForObject("select " + column + " from import_job where id = ?", Long.class, UUID.fromString(jobId));
        return value == null ? 0 : value;
    }

    private long count(String fromWhere) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + fromWhere, Long.class);
        return count == null ? 0 : count;
    }

    private String loginAccessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
