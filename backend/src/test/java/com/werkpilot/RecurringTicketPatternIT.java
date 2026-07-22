package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
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
class RecurringTicketPatternIT extends PostgreSqlTestContainerSupport {

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
    }

    @Test
    void threeTicketsForSameMachineAndCategoryWithinThirtyDaysCreateOneRecurringPatternAnomaly() throws Exception {
        String token = loginAccessToken();
        UUID machineId = UUID.randomUUID();

        createTicket(token, machineId, "HYDRAULICS");
        createTicket(token, machineId, "HYDRAULICS");
        assertThat(recurringAnomalyCount(machineId)).isZero();

        createTicket(token, machineId, "HYDRAULICS");
        assertThat(recurringAnomalyCount(machineId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select observed_value from anomaly where anomaly_type = 'RECURRING_TICKET_PATTERN' and machine_id = ?",
                java.math.BigDecimal.class,
                machineId)).isEqualByComparingTo("3.000");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from anomaly_recommendation where template_code = 'RECURRING_TICKET_ROOT_CAUSE'",
                Long.class)).isEqualTo(1L);

        createTicket(token, machineId, "HYDRAULICS");
        assertThat(recurringAnomalyCount(machineId)).isEqualTo(1L);
    }

    private void createTicket(String token, UUID machineId, String issueCategory) throws Exception {
        mockMvc.perform(post("/maintenance-tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hydraulik pruefen",
                                  "issueCategory": "%s",
                                  "priority": "HIGH",
                                  "machineId": "%s"
                                }
                                """.formatted(issueCategory, machineId)))
                .andExpect(status().isCreated());
    }

    private long recurringAnomalyCount(UUID machineId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from anomaly where anomaly_type = 'RECURRING_TICKET_PATTERN' and machine_id = ?",
                Long.class,
                machineId);
        return count == null ? 0 : count;
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
