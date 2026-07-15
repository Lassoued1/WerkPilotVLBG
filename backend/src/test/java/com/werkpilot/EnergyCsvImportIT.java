package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class EnergyCsvImportIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void validEnergyCsvCommitsLineAndMachineMeasurementsAtomically() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        Fixture fixture = createFixture(adminToken);

        MvcResult result = mockMvc.perform(multipart("/import-jobs/energy-measurements")
                        .file(csv("energy-valid.csv", """
                                period_start,period_end,factory_code,line_code,machine_code,shift_code,energy_kwh
                                2026-07-01T08:00:00Z,2026-07-01T09:00:00Z,%s,%s,%s,%s,12.345
                                2026-07-01T09:00:00Z,2026-07-01T10:00:00Z,%s,%s,,%s,20.000
                                """.formatted(
                                fixture.factoryCode(), fixture.lineCode(), fixture.machineCode(), fixture.shiftCode(),
                                fixture.factoryCode(), fixture.lineCode(), fixture.shiftCode())))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn();

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.jobId");

        mockMvc.perform(get("/import-jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(jobId))
                .andExpect(jsonPath("$.items[0].status").value("COMMITTED"))
                .andExpect(jsonPath("$.items[0].totalRows").value(2))
                .andExpect(jsonPath("$.items[0].validRows").value(2))
                .andExpect(jsonPath("$.items[0].errorCount").value(0));

        assertThat(count("energy_measurement where import_job_id = '%s'".formatted(jobId))).isEqualTo(2);
        assertThat(count("energy_measurement where import_job_id = '%s' and machine_id = '%s' and energy_kwh = 12.345".formatted(jobId, fixture.machineId()))).isEqualTo(1);
        assertThat(count("energy_measurement where import_job_id = '%s' and machine_id is null and energy_kwh = 20.000".formatted(jobId))).isEqualTo(1);
        assertThat(count("audit_event where event_type = 'CSV_IMPORT_COMMITTED' and details like '%" + jobId + "%'" )).isEqualTo(1);
    }

    @Test
    void invalidEnergyCsvRejectsAllRowsAndStoresGermanErrors() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        Fixture fixture = createFixture(adminToken);

        MvcResult result = mockMvc.perform(multipart("/import-jobs/energy-measurements")
                        .file(csv("energy-invalid.csv", """
                                period_start,period_end,factory_code,line_code,machine_code,shift_code,energy_kwh
                                2026-07-01T10:00:00Z,2026-07-01T09:00:00Z,%s,%s,UNKNOWN,%s,-1
                                """.formatted(fixture.factoryCode(), fixture.lineCode(), fixture.shiftCode())))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn();

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.jobId");

        mockMvc.perform(get("/import-jobs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(jobId))
                .andExpect(jsonPath("$.items[0].status").value("FAILED"))
                .andExpect(jsonPath("$.items[0].errorCount").value(1));

        mockMvc.perform(get("/import-jobs/{id}/errors", jobId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].rowNumber").value(2))
                .andExpect(jsonPath("$.items[*].message").isNotEmpty());

        assertThat(count("energy_measurement where import_job_id = '%s'".formatted(jobId))).isZero();
        assertThat(count("audit_event where event_type = 'CSV_IMPORT_FAILED' and details like '%" + jobId + "%'" )).isEqualTo(1);
    }

    private Fixture createFixture(String adminToken) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String factoryCode = "FE" + suffix;
        String lineCode = "LE" + suffix;
        String machineCode = "ME" + suffix;
        String shiftCode = "SE" + suffix;
        String factoryId = createFactory(adminToken, factoryCode);
        String lineId = createLine(adminToken, factoryId, lineCode);
        String machineId = createMachine(adminToken, lineId, machineCode);
        createShift(adminToken, shiftCode);
        return new Fixture(factoryCode, lineCode, machineCode, shiftCode, factoryId, lineId, machineId);
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

    private String createMachine(String adminToken, String lineId, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/machines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lineId":"%s","code":"%s","name":"Machine %s"}
                                """.formatted(lineId, code, code)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
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

    private record Fixture(String factoryCode, String lineCode, String machineCode, String shiftCode, String factoryId, String lineId, String machineId) {
    }
}
