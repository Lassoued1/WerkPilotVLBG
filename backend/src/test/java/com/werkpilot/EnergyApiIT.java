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
class EnergyApiIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void energyKpisUseIndependentCommittedEnergyAndProductionSums() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var productionJob = support.importJob("PRODUCTION_RECORDS", "COMMITTED");
        var energyJob = support.importJob("ENERGY_MEASUREMENTS", "COMMITTED");
        support.production(productionJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 100);
        support.energy(energyJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), new BigDecimal("50.000"));

        mockMvc.perform(get("/energy/kpis")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T10:00:00Z")
                        .param("lineId", fixture.lineId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnergyKwh").value(50.000))
                .andExpect(jsonPath("$.energyPerUnit.available").value(true))
                .andExpect(jsonPath("$.energyPerUnit.value").value(0.500))
                .andExpect(jsonPath("$.appliedFilters.lineId").value(fixture.lineId().toString()));
    }

    @Test
    void energyPerUnitIsUnavailableWhenNoUnitsWereProduced() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var energyJob = support.importJob("ENERGY_MEASUREMENTS", "COMMITTED");
        support.energy(energyJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), new BigDecimal("50.000"));

        mockMvc.perform(get("/energy/kpis")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T10:00:00Z")
                        .param("lineId", fixture.lineId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnergyKwh").value(50.000))
                .andExpect(jsonPath("$.energyPerUnit.available").value(false))
                .andExpect(jsonPath("$.energyPerUnit.reason").value("NO_UNITS_PRODUCED"));
    }

    @Test
    void energyTopConsumersReturnsHighestCommittedConsumersFirst() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var energyJob = support.importJob("ENERGY_MEASUREMENTS", "COMMITTED");
        support.energy(energyJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), new BigDecimal("75.000"));

        mockMvc.perform(get("/energy/top-consumers")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T10:00:00Z")
                        .param("lineId", fixture.lineId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lineId").value(fixture.lineId().toString()))
                .andExpect(jsonPath("$[0].machineId").value(fixture.machineId().toString()))
                .andExpect(jsonPath("$[0].energyKwh").value(75.000));
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
