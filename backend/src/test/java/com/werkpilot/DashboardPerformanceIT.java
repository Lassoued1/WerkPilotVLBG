package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import com.werkpilot.support.S3MeasurementFixtureSupport;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
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
class DashboardPerformanceIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void dashboardSummaryHasReusablePerformanceIndexesAndRunsThroughApiHarness() throws Exception {
        String token = loginAccessToken();
        assertThat(indexNames()).contains(
                "ix_production_record_dashboard_filters",
                "ix_energy_measurement_dashboard_filters",
                "ix_downtime_record_dashboard_filters",
                "ix_scrap_record_dashboard_filters");

        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var productionJob = support.importJob("PRODUCTION_RECORDS", "COMMITTED");
        support.production(productionJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 50);

        mockMvc.perform(get("/dashboard/summary")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T09:00:00Z")
                        .param("factoryId", fixture.factoryId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnitsProduced").value(50))
                .andExpect(jsonPath("$.productionTrend.length()").value(1));
    }

    private Set<String> indexNames() {
        return jdbcTemplate.queryForList(
                        "select indexname from pg_indexes where schemaname = 'public'",
                        String.class)
                .stream()
                .collect(Collectors.toSet());
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
