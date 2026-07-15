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
class ScrapCsvImportIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void validScrapCsvCommitsAtomicallyAndIsTraceable() throws Exception {
        String token = loginAccessToken();
        Fixture fixture = createFixture(token);
        MvcResult result = mockMvc.perform(multipart("/import-jobs/scrap-records")
                        .file(csv("scrap-valid.csv", """
                                period_start,period_end,machine_code,product_code,shift_code,scrap_count,scrap_category_code
                                2026-07-05T08:00:00Z,2026-07-05T09:00:00Z,%s,%s,%s,3,%s
                                2026-07-05T09:00:00Z,2026-07-05T10:00:00Z,%s,%s,%s,7,%s
                                """.formatted(fixture.machineCode(), fixture.productCode(), fixture.shiftCode(), fixture.categoryCode(), fixture.machineCode(), fixture.productCode(), fixture.shiftCode(), fixture.categoryCode())))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn();
        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.jobId");
        assertCommitted(token, jobId, "SCRAP_RECORDS", 2);
        assertThat(count("scrap_record where import_job_id = '%s'".formatted(jobId))).isEqualTo(2);
        assertThat(count("scrap_record where import_job_id = '%s' and machine_id = '%s' and product_id = '%s' and scrap_category_id = '%s' and scrap_count = 3".formatted(jobId, fixture.machineId(), fixture.productId(), fixture.categoryId()))).isEqualTo(1);
    }

    @Test
    void invalidScrapCsvRejectsAllRowsAndStoresErrors() throws Exception {
        String token = loginAccessToken();
        Fixture fixture = createFixture(token);
        MvcResult result = mockMvc.perform(multipart("/import-jobs/scrap-records")
                        .file(csv("scrap-invalid.csv", """
                                period_start,period_end,machine_code,product_code,shift_code,scrap_count,scrap_category_code
                                2026-07-05T09:00:00Z,2026-07-05T08:00:00Z,UNKNOWN,%s,%s,-1,%s
                                """.formatted(fixture.productCode(), fixture.shiftCode(), fixture.categoryCode())))
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.jobId");
        mockMvc.perform(get("/import-jobs").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(jobId))
                .andExpect(jsonPath("$.items[0].status").value("FAILED"))
                .andExpect(jsonPath("$.items[0].errorCount").value(1));
        mockMvc.perform(get("/import-jobs/{id}/errors", jobId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].rowNumber").value(2));
        assertThat(count("scrap_record where import_job_id = '%s'".formatted(jobId))).isZero();
    }

    private void assertCommitted(String token, String jobId, String type, int rows) throws Exception {
        mockMvc.perform(get("/import-jobs").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(jobId))
                .andExpect(jsonPath("$.items[0].importType").value(type))
                .andExpect(jsonPath("$.items[0].status").value("COMMITTED"))
                .andExpect(jsonPath("$.items[0].totalRows").value(rows))
                .andExpect(jsonPath("$.items[0].validRows").value(rows));
    }

    private Fixture createFixture(String token) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String factoryId = createFactory(token, "FS" + suffix);
        String lineId = createLine(token, factoryId, "LS" + suffix);
        String machineCode = "MS" + suffix;
        String machineId = createMachine(token, lineId, machineCode);
        String productCode = "PS" + suffix;
        String productId = createProduct(token, productCode);
        String shiftCode = "SS" + suffix;
        createShift(token, shiftCode);
        String categoryCode = "CS" + suffix;
        String categoryId = createSimple(token, "/scrap-categories", categoryCode);
        return new Fixture(machineCode, productCode, shiftCode, categoryCode, machineId, productId, categoryId);
    }

    private String createFactory(String token, String code) throws Exception { return postForId(token, "/factories", "{\"code\":\"%s\",\"name\":\"Factory %s\"}".formatted(code, code)); }
    private String createLine(String token, String factoryId, String code) throws Exception { return postForId(token, "/production-lines", "{\"factoryId\":\"%s\",\"code\":\"%s\",\"name\":\"Line %s\"}".formatted(factoryId, code, code)); }
    private String createMachine(String token, String lineId, String code) throws Exception { return postForId(token, "/machines", "{\"lineId\":\"%s\",\"code\":\"%s\",\"name\":\"Machine %s\"}".formatted(lineId, code, code)); }
    private String createProduct(String token, String code) throws Exception { return postForId(token, "/products", "{\"code\":\"%s\",\"name\":\"Product %s\",\"family\":\"Pilot\"}".formatted(code, code)); }
    private String createSimple(String token, String path, String code) throws Exception { return postForId(token, path, "{\"code\":\"%s\",\"name\":\"Name %s\"}".formatted(code, code)); }
    private void createShift(String token, String code) throws Exception { postForId(token, "/shifts", "{\"code\":\"%s\",\"name\":\"Shift %s\",\"startTime\":\"06:00:00\",\"endTime\":\"14:00:00\",\"plannedMinutes\":480}".formatted(code, code)); }

    private String postForId(String token, String path, String json) throws Exception {
        MvcResult result = mockMvc.perform(post(path).header(HttpHeaders.AUTHORIZATION, bearer(token)).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
    private String loginAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD))).andExpect(status().isOk()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
    private long count(String fromWhere) { Long count = jdbcTemplate.queryForObject("select count(*) from " + fromWhere, Long.class); return count == null ? 0 : count; }
    private static MockMultipartFile csv(String filename, String content) { return new MockMultipartFile("file", filename, "text/csv", content.getBytes(StandardCharsets.UTF_8)); }
    private static String bearer(String token) { return "Bearer " + token; }
    private record Fixture(String machineCode, String productCode, String shiftCode, String categoryCode, String machineId, String productId, String categoryId) { }
}
