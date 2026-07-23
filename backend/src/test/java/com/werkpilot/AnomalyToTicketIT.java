package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
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
class AnomalyToTicketIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("delete from maintenance_ticket_comment");
        jdbcTemplate.update("delete from maintenance_ticket");
        jdbcTemplate.update("delete from anomaly_recommendation");
        jdbcTemplate.update("delete from anomaly");
        jdbcTemplate.update("delete from audit_event where event_type = 'ANOMALY_STATUS_CHANGED'");
    }

    @Test
    void managerConvertsAnomalyIntoExactlyOneLinkedTicketAndAnomalyStatusChanges() throws Exception {
        String token = loginAccessToken();
        UUID anomalyId = insertAnomaly();

        MvcResult result = mockMvc.perform(post("/anomalies/{id}/tickets", anomalyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Pruefung der Energieanomalie",
                                  "issueCategory": "ENERGY",
                                  "priority": "HIGH",
                                  "dueDate": "2026-07-25"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn();

        String ticketId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/anomalies/{id}", anomalyId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anomaly.status").value("LINKED_TO_TICKET"));

        mockMvc.perform(get("/anomalies/{id}/ticket", anomalyId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId));

        mockMvc.perform(get("/maintenance-tickets/{id}", ticketId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.anomalyId").value(anomalyId.toString()))
                .andExpect(jsonPath("$.ticket.issueCategory").value("ENERGY"));

        mockMvc.perform(post("/anomalies/{id}/tickets", anomalyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Duplicate","priority":"HIGH"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_event where event_type = 'ANOMALY_STATUS_CHANGED' and details like ?",
                Long.class,
                "%ticketId=" + ticketId + "%")).isEqualTo(1L);
    }

    private UUID insertAnomaly() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into anomaly
                        (id, identity_key, detector_version, metric_key, anomaly_type, severity, status, detection_method,
                         period_start, period_end, observed_value, baseline_average, baseline_stddev, baseline_count,
                         baseline_quality, explanation, fingerprint, created_at, updated_at)
                        values (?, ?, ?, 'ENERGY_KWH', 'ENERGY_SPIKE', 'CRITICAL', 'NEW', 'THRESHOLD',
                                ?, ?, ?, ?, ?, ?, 'LOW', ?, ?, now(), now())
                        """,
                id,
                "anomaly-to-ticket-" + id,
                "test-v1",
                Timestamp.from(Instant.parse("2026-07-20T08:00:00Z")),
                Timestamp.from(Instant.parse("2026-07-20T09:00:00Z")),
                new BigDecimal("200.000"),
                new BigDecimal("100.000"),
                new BigDecimal("0.000"),
                1,
                "Energy anomaly for conversion test.",
                "fingerprint-" + id);
        return id;
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
}
