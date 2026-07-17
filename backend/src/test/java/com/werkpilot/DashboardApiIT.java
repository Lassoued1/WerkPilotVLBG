package com.werkpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import com.werkpilot.support.S3MeasurementFixtureSupport;
import java.math.BigDecimal;
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
class DashboardApiIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void dashboardSummaryReturnsBackendCalculatedCardsAndTrends() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var productionJob = support.importJob("PRODUCTION_RECORDS", "COMMITTED");
        var energyJob = support.importJob("ENERGY_MEASUREMENTS", "COMMITTED");
        var downtimeJob = support.importJob("DOWNTIME_RECORDS", "COMMITTED");
        var scrapJob = support.importJob("SCRAP_RECORDS", "COMMITTED");
        support.production(productionJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 100);
        support.energy(energyJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), new BigDecimal("45.000"));
        support.downtime(downtimeJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T08:30:00Z"), 30);
        support.scrap(scrapJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T08:10:00Z"), 5);

        mockMvc.perform(get("/dashboard/summary")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T09:00:00Z")
                        .param("lineId", fixture.lineId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnitsProduced").value(100))
                .andExpect(jsonPath("$.totalEnergyKwh").value(45.000))
                .andExpect(jsonPath("$.totalDowntimeMinutes").value(30))
                .andExpect(jsonPath("$.totalScrapCount").value(5))
                .andExpect(jsonPath("$.outputPerHour.value").value(100.000))
                .andExpect(jsonPath("$.energyPerUnit.value").value(0.450))
                .andExpect(jsonPath("$.availability.value").value(93.750))
                .andExpect(jsonPath("$.scrapRate.value").value(5.000))
                .andExpect(jsonPath("$.productionTrend[0].unitsProduced").value(100))
                .andExpect(jsonPath("$.downtimePareto[0].downtimeMinutes").value(30))
                .andExpect(jsonPath("$.energyTopConsumers[0].energyKwh").value(45.000));
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
