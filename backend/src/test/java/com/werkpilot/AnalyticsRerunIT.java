package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import com.werkpilot.support.S3MeasurementFixtureSupport;
import java.math.BigDecimal;
import java.sql.Timestamp;
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
class AnalyticsRerunIT extends PostgreSqlTestContainerSupport {

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
        jdbcTemplate.update("delete from audit_event where event_type = 'ANOMALY_STATUS_CHANGED'");
    }

    @Test
    void adminRerunCreatesListDetailRecommendationsAndAllowsManagerStatusChange() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        UUID adminId = adminUserId();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        UUID energyJob = support.importJob("ENERGY_MEASUREMENTS", "COMMITTED");
        insertEnergyFixture(support, energyJob, fixture, new BigDecimal("200.000"));
        insertGlobalThreshold("ENERGY_KWH", "150.000", "CRITICAL", adminId);

        mockMvc.perform(post("/anomalies/rerun")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rerunBody(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.detected").value(1));

        MvcResult list = mockMvc.perform(get("/anomalies")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("anomalyType", "ENERGY_SPIKE")
                        .param("lineId", fixture.lineId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$.items[0].status").value("NEW"))
                .andExpect(jsonPath("$.items[0].detectionMethod").value("THRESHOLD"))
                .andReturn();

        String anomalyId = JsonPath.read(list.getResponse().getContentAsString(), "$.items[0].id");

        mockMvc.perform(get("/anomalies/{id}", anomalyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(1))
                .andExpect(jsonPath("$.recommendations[0].templateVersion").value("2026-07-s4-v1"))
                .andExpect(jsonPath("$.recommendations[0].disclaimerDe").value(com.werkpilot.analytics.application.RecommendationService.DISCLAIMER_DE));

        mockMvc.perform(patch("/anomalies/{id}/status", anomalyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACKNOWLEDGED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_event where event_type = 'ANOMALY_STATUS_CHANGED' and details like ?",
                Long.class,
                "%" + anomalyId + "%")).isEqualTo(1L);
    }

    @Test
    void rerunIsNoOpWhenDetectionFingerprintIsUnchanged() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        UUID adminId = adminUserId();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        UUID energyJob = support.importJob("ENERGY_MEASUREMENTS", "COMMITTED");
        insertEnergyFixture(support, energyJob, fixture, new BigDecimal("200.000"));
        insertGlobalThreshold("ENERGY_KWH", "150.000", "CRITICAL", adminId);

        mockMvc.perform(post("/anomalies/rerun")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rerunBody(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1));

        mockMvc.perform(post("/anomalies/rerun")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rerunBody(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.unchanged").value(1));
    }

    private void insertEnergyFixture(S3MeasurementFixtureSupport support, UUID energyJob, S3MeasurementFixtureSupport.Fixture fixture, BigDecimal observed) {
        support.energy(energyJob, fixture, Instant.parse("2026-07-15T08:00:00Z"), Instant.parse("2026-07-15T09:00:00Z"), observed);
        for (int index = 1; index <= 12; index++) {
            UUID baselineJob = support.importJob("ENERGY_MEASUREMENTS", "COMMITTED");
            Instant from = Instant.parse("2026-07-15T08:00:00Z").minusSeconds(3600L * index);
            support.energy(baselineJob, fixture, from, from.plusSeconds(3600), new BigDecimal("100.000"));
        }
    }

    private void insertGlobalThreshold(String metricKey, String maxValue, String severity, UUID adminId) {
        jdbcTemplate.update(
                """
                        insert into threshold_rule
                        (id, metric_key, scope_type, scope_id, min_value, max_value, severity, active, created_by_user_id, updated_by_user_id, created_at, updated_at)
                        values (?, ?, 'GLOBAL', null, null, ?, ?, true, ?, ?, now(), now())
                        """,
                UUID.randomUUID(),
                metricKey,
                new BigDecimal(maxValue),
                severity,
                adminId,
                adminId);
    }

    private String rerunBody(S3MeasurementFixtureSupport.Fixture fixture) {
        return """
                {
                  "from": "2026-07-15T08:00:00Z",
                  "to": "2026-07-15T09:00:00Z",
                  "lineId": "%s",
                  "machineId": "%s",
                  "shiftId": "%s"
                }
                """.formatted(fixture.lineId(), fixture.machineId(), fixture.shiftId());
    }

    private UUID adminUserId() {
        return jdbcTemplate.queryForObject("select id from app_user where email = ?", UUID.class, ADMIN_EMAIL);
    }

    private String loginAccessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
