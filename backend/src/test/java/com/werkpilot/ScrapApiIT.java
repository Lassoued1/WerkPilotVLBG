package com.werkpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ScrapApiIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void scrapRateUsesCommittedScrapAndProductionWithFilters() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var productionJob = support.importJob("PRODUCTION_RECORDS", "COMMITTED");
        var scrapJob = support.importJob("SCRAP_RECORDS", "COMMITTED");
        var supersededScrapJob = support.importJob("SCRAP_RECORDS", "SUPERSEDED");
        support.production(productionJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 200);
        support.scrap(scrapJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T08:10:00Z"), 8);
        support.scrap(supersededScrapJob, fixture, instant("2026-07-01T08:10:00Z"), instant("2026-07-01T08:20:00Z"), 99);

        mockMvc.perform(get("/quality/scrap-rate")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T09:00:00Z")
                        .param("productId", fixture.productId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScrapCount").value(8))
                .andExpect(jsonPath("$.totalUnitsProduced").value(200))
                .andExpect(jsonPath("$.scrapRate.available").value(true))
                .andExpect(jsonPath("$.scrapRate.value").value(4.000))
                .andExpect(jsonPath("$.appliedFilters.productId").value(fixture.productId().toString()));
    }

    @Test
    void scrapRateIsUnavailableWithoutProducedUnits() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var scrapJob = support.importJob("SCRAP_RECORDS", "COMMITTED");
        support.scrap(scrapJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T08:10:00Z"), 8);

        mockMvc.perform(get("/quality/scrap-rate")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T09:00:00Z")
                        .param("productId", fixture.productId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScrapCount").value(8))
                .andExpect(jsonPath("$.totalUnitsProduced").value(0))
                .andExpect(jsonPath("$.scrapRate.available").value(false))
                .andExpect(jsonPath("$.scrapRate.reason").value("NO_UNITS_PRODUCED"));
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
