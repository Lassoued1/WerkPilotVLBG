package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class ThresholdAdministrationIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";
    private static final String USER_PASSWORD = "User-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetThresholdState() {
        jdbcTemplate.update("delete from threshold_rule");
        jdbcTemplate.update("delete from audit_event where event_type in ('THRESHOLD_CHANGED', 'THRESHOLD_DELEGATION_CHANGED')");
        jdbcTemplate.update("update system_settings set energy_threshold_delegation_enabled = false, updated_by_user_id = null, updated_at = now() where id = 1");
    }

    @Test
    void adminCreatesUpdatesListsDeletesScopedThresholdsAndAuditsChanges() throws Exception {
        MvcResult adminLogin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String adminToken = JsonPath.read(adminLogin.getResponse().getContentAsString(), "$.accessToken");
        String adminId = JsonPath.read(adminLogin.getResponse().getContentAsString(), "$.profile.id");
        String factoryId = createFactory(adminToken);

        MvcResult createResult = mockMvc.perform(post("/thresholds")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metricKey": "OUTPUT_PER_HOUR",
                                  "scopeType": "FACTORY",
                                  "scopeId": "%s",
                                  "minValue": 80.0,
                                  "maxValue": 120.0,
                                  "severity": "WARNING",
                                  "active": true
                                }
                                """.formatted(factoryId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("/thresholds/")))
                .andExpect(jsonPath("$.metricKey").value("OUTPUT_PER_HOUR"))
                .andExpect(jsonPath("$.scopeType").value("FACTORY"))
                .andExpect(jsonPath("$.scopeId").value(factoryId))
                .andExpect(jsonPath("$.severity").value("WARNING"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdByUserId").value(adminId))
                .andReturn();

        String thresholdId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/thresholds/{id}", thresholdId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metricKey": "OUTPUT_PER_HOUR",
                                  "scopeType": "FACTORY",
                                  "scopeId": "%s",
                                  "minValue": 90.0,
                                  "maxValue": 130.0,
                                  "severity": "WARNING",
                                  "active": true
                                }
                                """.formatted(factoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minValue").value(90.0))
                .andExpect(jsonPath("$.maxValue").value(130.0));

        mockMvc.perform(get("/thresholds")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("metricKey", "OUTPUT_PER_HOUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(thresholdId));

        mockMvc.perform(delete("/thresholds/{id}", thresholdId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/thresholds/{id}", thresholdId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(auditCount("THRESHOLD_CHANGED", adminId, thresholdId)).isEqualTo(3);
    }

    @Test
    void energyManagerWritesEnergyThresholdsOnlyWhenDelegationIsOn() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        String energyEmail = "energy.threshold.%s@werkpilot.local".formatted(suffix().toLowerCase());
        createUser(adminToken, energyEmail, "ENERGY_MANAGER");
        String energyToken = loginAccessToken(energyEmail, USER_PASSWORD);

        mockMvc.perform(post("/thresholds")
                        .header(HttpHeaders.AUTHORIZATION, bearer(energyToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyThresholdJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/settings/global/energy-threshold-delegation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/thresholds")
                        .header(HttpHeaders.AUTHORIZATION, bearer(energyToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(energyThresholdJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metricKey").value("ENERGY_PER_UNIT"))
                .andExpect(jsonPath("$.scopeType").value("GLOBAL"));

        mockMvc.perform(post("/thresholds")
                        .header(HttpHeaders.AUTHORIZATION, bearer(energyToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metricKey": "SCRAP_RATE",
                                  "scopeType": "GLOBAL",
                                  "maxValue": 3.0,
                                  "severity": "CRITICAL",
                                  "active": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidScopeAndDuplicateActiveDefinitionAreRejected() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        String unknownFactoryId = UUID.randomUUID().toString();

        mockMvc.perform(post("/thresholds")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metricKey": "OUTPUT_PER_HOUR",
                                  "scopeType": "FACTORY",
                                  "scopeId": "%s",
                                  "maxValue": 100.0,
                                  "severity": "WARNING",
                                  "active": true
                                }
                                """.formatted(unknownFactoryId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(post("/thresholds")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metricKey": "SCRAP_RATE",
                                  "scopeType": "GLOBAL",
                                  "maxValue": 2.5,
                                  "severity": "CRITICAL",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/thresholds")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metricKey": "SCRAP_RATE",
                                  "scopeType": "GLOBAL",
                                  "maxValue": 3.0,
                                  "severity": "CRITICAL",
                                  "active": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    private String createFactory(String adminToken) throws Exception {
        String code = "THR-" + suffix().toUpperCase();
        MvcResult result = mockMvc.perform(post("/factories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "Threshold Factory"
                                }
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void createUser(String adminToken, String email, String role) throws Exception {
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "Threshold User",
                                  "temporaryPassword": "%s",
                                  "roles": ["%s"]
                                }
                                """.formatted(email, USER_PASSWORD, role)))
                .andExpect(status().isCreated());
    }

    private String loginAccessToken(String email, String password) throws Exception {
        return JsonPath.read(login(email, password).getResponse().getContentAsString(), "$.accessToken");
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private long auditCount(String eventType, String actorUserId, String thresholdId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from audit_event
                        where event_type = ?
                          and actor_user_id = ?::uuid
                          and details like ?
                        """,
                Long.class,
                eventType,
                actorUserId,
                "%" + thresholdId + "%");
        return count == null ? 0 : count;
    }

    private static String energyThresholdJson() {
        return """
                {
                  "metricKey": "ENERGY_PER_UNIT",
                  "scopeType": "GLOBAL",
                  "maxValue": 7.5,
                  "severity": "CRITICAL",
                  "active": true
                }
                """;
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
