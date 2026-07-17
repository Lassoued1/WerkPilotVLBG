package com.werkpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import com.werkpilot.support.S3MeasurementFixtureSupport;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ProductionApiIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void productionKpisReturnCommittedAggregatesAndAppliedFilters() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var job = support.importJob("PRODUCTION_RECORDS", "COMMITTED");
        support.production(job, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 30);
        support.production(job, fixture, instant("2026-07-01T09:00:00Z"), instant("2026-07-01T10:00:00Z"), 90);

        mockMvc.perform(get("/production/kpis")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T10:00:00Z")
                        .param("factoryId", fixture.factoryId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnitsProduced").value(120))
                .andExpect(jsonPath("$.outputPerHour.available").value(true))
                .andExpect(jsonPath("$.outputPerHour.value").value(60.000))
                .andExpect(jsonPath("$.appliedFilters.factoryId").value(fixture.factoryId().toString()));
    }

    @Test
    void productionRecordsArePaginatedAndFiltered() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var job = support.importJob("PRODUCTION_RECORDS", "COMMITTED");
        support.production(job, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 30);
        support.production(job, fixture, instant("2026-07-01T09:00:00Z"), instant("2026-07-01T10:00:00Z"), 90);

        mockMvc.perform(get("/production/records")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T10:00:00Z")
                        .param("machineId", fixture.machineId().toString())
                        .param("page", "0")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].unitsProduced").value(30));
    }

    @Test
    void productionTrendAndEvidenceCsvAreReturnedFromCommittedRows() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var job = support.importJob("PRODUCTION_RECORDS", "COMMITTED");
        support.production(job, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 30);
        support.production(job, fixture, instant("2026-07-01T08:30:00Z"), instant("2026-07-01T09:00:00Z"), 20);

        mockMvc.perform(get("/production/trends")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T10:00:00Z")
                        .param("factoryId", fixture.factoryId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bucketStart").value("2026-07-01T08:00:00Z"))
                .andExpect(jsonPath("$[0].unitsProduced").value(50));

        mockMvc.perform(get("/production/evidence.csv")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T10:00:00Z")
                        .param("factoryId", fixture.factoryId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("period_start,period_end")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(",30,\"BATCH-S3\",")));
    }

    private String loginAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static Instant instant(String value) {
        return Instant.parse(value);
    }
}
