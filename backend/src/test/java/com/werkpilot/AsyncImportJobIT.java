package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.importing.application.ImportJobService;
import com.werkpilot.importing.application.port.ImportJobErrorRecord;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
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
class AsyncImportJobIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ImportJobService importJobService;

    @Test
    void energyManagerStartsProcessingJobAndCanPollHistoryAndErrors() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        createUser(adminToken, "energy.importer@werkpilot.local", "Energy Importer", "ENERGY_MANAGER");
        String managerToken = loginAccessToken("energy.importer@werkpilot.local", "Importer-Change-Me-2026");

        long startedAuditBefore = auditCount("CSV_IMPORT_STARTED");

        MvcResult startResult = mockMvc.perform(multipart("/import-jobs/energy-measurements")
                        .file(csv("../unsafe/Energy File.csv", "machine_code,units_produced\nM-01,5\n"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        String jobId = JsonPath.read(startResult.getResponse().getContentAsString(), "$.jobId");

        mockMvc.perform(get("/import-jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(jobId))
                .andExpect(jsonPath("$.items[0].importType").value("ENERGY_MEASUREMENTS"))
                .andExpect(jsonPath("$.items[0].status").value("FAILED"))
                .andExpect(jsonPath("$.items[0].safeFilename").value("energy_file.csv"));
        assertThat(auditCount("CSV_IMPORT_STARTED")).isEqualTo(startedAuditBefore + 1);

        List<ImportJobErrorRecord> errors = IntStream.rangeClosed(1, 501)
                .mapToObj(row -> new ImportJobErrorRecord(
                        UUID.randomUUID(),
                        UUID.fromString(jobId),
                        row,
                        "units_produced",
                        "-1",
                        "Der Wert muss grÃƒÂ¶ÃƒÅ¸er oder gleich null sein.",
                        null))
                .toList();
        importJobService.replaceErrorsForFailedJob(UUID.fromString(jobId), errors, 501);

        mockMvc.perform(get("/import-jobs/{id}/errors", jobId)
                        .param("size", "600")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(500))
                .andExpect(jsonPath("$.items[0].rowNumber").value(1))
                .andExpect(jsonPath("$.items[0].message").value("Der Wert muss grÃƒÂ¶ÃƒÅ¸er oder gleich null sein."));

        mockMvc.perform(get("/import-jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("FAILED"))
                .andExpect(jsonPath("$.items[0].errorCount").value(501))
                .andExpect(jsonPath("$.items[0].errorOverflow").value(true));
    }

    @Test
    void duplicateNormalImportHashIsRejectedForSameType() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        MockMultipartFile file = csv("production.csv", "machine_code,units_produced\nM-02,6\n");

        mockMvc.perform(multipart("/import-jobs/energy-measurements")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/import-jobs/energy-measurements")
                        .file(csv("copy.csv", "machine_code,units_produced\nM-02,6\n"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IMPORT_DUPLICATE_FILE"));
    }

    @Test
    void roleMatrixProtectsImportStartEndpoints() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        createUser(adminToken, "viewer.imports@werkpilot.local", "Viewer Imports", "VIEWER");
        createUser(adminToken, "energy.imports@werkpilot.local", "Energy Imports", "ENERGY_MANAGER");
        String viewerToken = loginAccessToken("viewer.imports@werkpilot.local", "Importer-Change-Me-2026");
        String energyToken = loginAccessToken("energy.imports@werkpilot.local", "Importer-Change-Me-2026");

        mockMvc.perform(multipart("/import-jobs/production-records")
                        .file(csv("production.csv", "a,b\n")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(multipart("/import-jobs/production-records")
                        .file(csv("production.csv", "a,b\n"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(viewerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/import-jobs/energy-measurements")
                        .file(csv("energy.csv", "a,b\n"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(energyToken)))
                .andExpect(status().isOk());

        mockMvc.perform(multipart("/import-jobs/production-records")
                        .file(csv("production-2.csv", "a,b\n"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(energyToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingJobErrorsEndpointReturnsNotFound() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/import-jobs/{id}/errors", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
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

    private long countRows(String tableName) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        return count == null ? 0 : count;
    }

    private long auditCount(String eventType) {
        Long count = jdbcTemplate.queryForObject("select count(*) from audit_event where event_type = ?", Long.class, eventType);
        return count == null ? 0 : count;
    }

    private static MockMultipartFile csv(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/csv", content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}




