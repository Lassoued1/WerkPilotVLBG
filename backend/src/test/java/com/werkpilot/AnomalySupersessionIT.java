package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import com.werkpilot.support.S3MeasurementFixtureSupport;
import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AnomalySupersessionIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAnalytics() {
        jdbcTemplate.update("delete from anomaly_recommendation");
        jdbcTemplate.update("delete from anomaly");
        jdbcTemplate.update("delete from threshold_rule");
    }

    @Test
    void changedResultSupersedesOldAnomalyAndLinksNewSuccessor() throws Exception {
        String token = loginAccessToken();
        Scenario scenario = scenario(new BigDecimal("200.000"));
        rerun(token, scenario.fixture()).andExpect(jsonPath("$.created").value(1));

        UUID firstId = activeAnomalyId();
        jdbcTemplate.update("update energy_measurement set energy_kwh = ? where import_job_id = ?", new BigDecimal("260.000"), scenario.currentEnergyJob());

        rerun(token, scenario.fixture())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.superseded").value(1));

        UUID secondId = activeAnomalyId();
        assertThat(secondId).isNotEqualTo(firstId);
        assertThat(jdbcTemplate.queryForObject("select status from anomaly where id = ?", String.class, firstId)).isEqualTo("SUPERSEDED");
        assertThat(jdbcTemplate.queryForObject("select previous_anomaly_id from anomaly where id = ?", UUID.class, secondId)).isEqualTo(firstId);
        assertThat(jdbcTemplate.queryForObject("select superseded_by_anomaly_id from anomaly where id = ?", UUID.class, firstId)).isEqualTo(secondId);
    }

    @Test
    void disappearedResultSupersedesWithoutSuccessor() throws Exception {
        String token = loginAccessToken();
        Scenario scenario = scenario(new BigDecimal("200.000"));
        rerun(token, scenario.fixture()).andExpect(jsonPath("$.created").value(1));
        UUID firstId = activeAnomalyId();

        jdbcTemplate.update("update threshold_rule set active = false");
        jdbcTemplate.update("update energy_measurement set energy_kwh = ? where import_job_id = ?", new BigDecimal("100.000"), scenario.currentEnergyJob());

        rerun(token, scenario.fixture())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.superseded").value(1));

        assertThat(jdbcTemplate.queryForObject("select status from anomaly where id = ?", String.class, firstId)).isEqualTo("SUPERSEDED");
        assertThat(jdbcTemplate.queryForObject("select superseded_by_anomaly_id from anomaly where id = ?", UUID.class, firstId)).isNull();
    }

    private Scenario scenario(BigDecimal observed) {
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        UUID energyJob = support.importJob("ENERGY_MEASUREMENTS", "COMMITTED");
        support.energy(energyJob, fixture, Instant.parse("2026-07-15T08:00:00Z"), Instant.parse("2026-07-15T09:00:00Z"), observed);
        for (int index = 1; index <= 12; index++) {
            UUID baselineJob = support.importJob("ENERGY_MEASUREMENTS", "COMMITTED");
            Instant from = Instant.parse("2026-07-15T08:00:00Z").minusSeconds(3600L * index);
            support.energy(baselineJob, fixture, from, from.plusSeconds(3600), new BigDecimal("100.000"));
        }
        UUID adminId = jdbcTemplate.queryForObject("select id from app_user where email = ?", UUID.class, ADMIN_EMAIL);
        jdbcTemplate.update(
                """
                        insert into threshold_rule
                        (id, metric_key, scope_type, scope_id, min_value, max_value, severity, active, created_by_user_id, updated_by_user_id, created_at, updated_at)
                        values (?, 'ENERGY_KWH', 'GLOBAL', null, null, 150.000, 'CRITICAL', true, ?, ?, now(), now())
                        """,
                UUID.randomUUID(),
                adminId,
                adminId);
        return new Scenario(fixture, energyJob);
    }

    private org.springframework.test.web.servlet.ResultActions rerun(String token, S3MeasurementFixtureSupport.Fixture fixture) throws Exception {
        return mockMvc.perform(post("/anomalies/rerun")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "from": "2026-07-15T08:00:00Z",
                          "to": "2026-07-15T09:00:00Z",
                          "lineId": "%s",
                          "machineId": "%s",
                          "shiftId": "%s"
                        }
                        """.formatted(fixture.lineId(), fixture.machineId(), fixture.shiftId())))
                .andExpect(status().isOk());
    }

    private UUID activeAnomalyId() {
        return jdbcTemplate.queryForObject("select id from anomaly where status <> 'SUPERSEDED'", UUID.class);
    }

    private String loginAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Scenario(S3MeasurementFixtureSupport.Fixture fixture, UUID currentEnergyJob) {
    }
}
