package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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

@SpringBootTest
@AutoConfigureMockMvc
class ImportRollbackIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rollbackSupersedesCommittedJobAndPersistsReason() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        Fixture fixture = createFixture(adminToken);
        String jobId = startProductionImport(adminToken, fixture);

        assertThat(count("production_record pr join import_job ij on ij.id = pr.import_job_id where ij.status = 'COMMITTED' and pr.import_job_id = '%s'".formatted(jobId))).isEqualTo(1);

        mockMvc.perform(post("/import-jobs/{id}/rollback", jobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Falsche Schicht wurde importiert."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.status").value("SUPERSEDED"));

        assertThat(jobStatus(jobId)).isEqualTo("SUPERSEDED");
        assertThat(supersededReason(jobId)).isEqualTo("Falsche Schicht wurde importiert.");
        assertThat(count("audit_event where event_type = 'CSV_IMPORT_ROLLED_BACK' and details like '%" + jobId + "%' and details like '%Falsche Schicht wurde importiert.%'")).isEqualTo(1);

        assertThat(count("production_record pr join import_job ij on ij.id = pr.import_job_id where ij.status = 'COMMITTED' and pr.import_job_id = '%s'".formatted(jobId))).isZero();
    }

    @Test
    void rollbackRequiresNonBlankReason() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        Fixture fixture = createFixture(adminToken);
        String jobId = startProductionImport(adminToken, fixture);

        mockMvc.perform(post("/import-jobs/{id}/rollback", jobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"   "}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(jobStatus(jobId)).isEqualTo("COMMITTED");
    }

    @Test
    void rollbackRejectsNonCommittedTarget() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        Fixture fixture = createFixture(adminToken);

        MvcResult failedResult = mockMvc.perform(multipart("/import-jobs/production-records")
                        .file(csv("production-invalid.csv", invalidProductionCsv(fixture)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        String failedJobId = JsonPath.read(failedResult.getResponse().getContentAsString(), "$.jobId");
        assertThat(jobStatus(failedJobId)).isEqualTo("FAILED");

        mockMvc.perform(post("/import-jobs/{id}/rollback", failedJobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Should not apply."}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IMPORT_JOB_NOT_ELIGIBLE"));
    }

    @Test
    void nonAdminCallerCannotRollbackImport() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        Fixture fixture = createFixture(adminToken);
        String jobId = startProductionImport(adminToken, fixture);

        createUser(adminToken, "manager.rollback@werkpilot.local", "Manager Rollback", "PRODUCTION_MANAGER");
        String managerToken = loginAccessToken("manager.rollback@werkpilot.local", "Importer-Change-Me-2026");

        mockMvc.perform(post("/import-jobs/{id}/rollback", jobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Attempted rollback."}
                                """))
                .andExpect(status().isForbidden());

        assertThat(jobStatus(jobId)).isEqualTo("COMMITTED");
    }

    private String startProductionImport(String adminToken, Fixture fixture) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/import-jobs/production-records")
                        .file(csv("production-valid.csv", productionCsv(fixture)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn();
        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.jobId");
        assertThat(jobStatus(jobId)).isEqualTo("COMMITTED");
        return jobId;
    }

    private static String productionCsv(Fixture fixture) {
        return """
                period_start,period_end,factory_code,line_code,machine_code,product_code,shift_code,units_produced,batch_code
                2026-07-01T08:00:00Z,2026-07-01T09:00:00Z,%s,%s,%s,%s,%s,42,BATCH-01
                """.formatted(fixture.factoryCode(), fixture.lineCode(), fixture.machineCode(), fixture.productCode(), fixture.shiftCode());
    }

    private static String invalidProductionCsv(Fixture fixture) {
        return """
                period_start,period_end,factory_code,line_code,machine_code,product_code,shift_code,units_produced,batch_code
                2026-07-01T10:00:00Z,2026-07-01T09:00:00Z,%s,%s,UNKNOWN,%s,%s,-1,BATCH-INVALID
                """.formatted(fixture.factoryCode(), fixture.lineCode(), fixture.productCode(), fixture.shiftCode());
    }

    private Fixture createFixture(String adminToken) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String factoryCode = "F" + suffix;
        String lineCode = "L" + suffix;
        String machineCode = "M" + suffix;
        String productCode = "P" + suffix;
        String shiftCode = "S" + suffix;
        String factoryId = createFactory(adminToken, factoryCode);
        String lineId = createLine(adminToken, factoryId, lineCode);
        createMachine(adminToken, lineId, machineCode);
        createProduct(adminToken, productCode);
        createShift(adminToken, shiftCode);
        return new Fixture(factoryCode, lineCode, machineCode, productCode, shiftCode);
    }

    private String createFactory(String adminToken, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/factories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Factory %s"}
                                """.formatted(code, code)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createLine(String adminToken, String factoryId, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/production-lines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"factoryId":"%s","code":"%s","name":"Line %s"}
                                """.formatted(factoryId, code, code)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void createMachine(String adminToken, String lineId, String code) throws Exception {
        mockMvc.perform(post("/machines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lineId":"%s","code":"%s","name":"Machine %s"}
                                """.formatted(lineId, code, code)))
                .andExpect(status().isCreated());
    }

    private void createProduct(String adminToken, String code) throws Exception {
        mockMvc.perform(post("/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Product %s","family":"Pilot"}
                                """.formatted(code, code)))
                .andExpect(status().isCreated());
    }

    private void createShift(String adminToken, String code) throws Exception {
        mockMvc.perform(post("/shifts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Shift %s","startTime":"06:00:00","endTime":"14:00:00","plannedMinutes":480}
                                """.formatted(code, code)))
                .andExpect(status().isCreated());
    }

    private void createUser(String adminToken, String email, String displayName, String role) throws Exception {
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "%s",
                                  "temporaryPassword": "Importer-Change-Me-2026",
                                  "roles": ["%s"]
                                }
                                """.formatted(email, displayName, role)))
                .andExpect(status().isCreated());
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

    private String jobStatus(String jobId) {
        return jdbcTemplate.queryForObject("select status from import_job where id = ?", String.class, UUID.fromString(jobId));
    }

    private String supersededReason(String jobId) {
        return jdbcTemplate.queryForObject("select superseded_reason from import_job where id = ?", String.class, UUID.fromString(jobId));
    }

    private long count(String fromWhere) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + fromWhere, Long.class);
        return count == null ? 0 : count;
    }

    private static MockMultipartFile csv(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Fixture(
            String factoryCode,
            String lineCode,
            String machineCode,
            String productCode,
            String shiftCode) {
    }
}
