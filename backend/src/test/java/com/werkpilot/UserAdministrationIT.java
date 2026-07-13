package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
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
class UserAdministrationIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adminCreatesListsUpdatesDisablesUsersAndAuditsChanges() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        MvcResult createResult = mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Planner@WerkPilot.Local",
                                  "displayName": "Production Planner",
                                  "temporaryPassword": "Planner-Change-Me-2026",
                                  "roles": ["VIEWER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("/users/")))
                .andExpect(jsonPath("$.email").value("planner@werkpilot.local"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roles[0]").value("VIEWER"))
                .andReturn();

        String userId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.email == 'planner@werkpilot.local')].displayName").value("Production Planner"));

        mockMvc.perform(put("/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Energy Coordinator",
                                  "active": true,
                                  "roles": ["ENERGY_MANAGER"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Energy Coordinator"))
                .andExpect(jsonPath("$.roles[0]").value("ENERGY_MANAGER"));

        mockMvc.perform(patch("/users/{id}/status", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(auditCount("USER_CREATED", userId)).isEqualTo(1);
        assertThat(auditCount("USER_ROLE_CHANGED", userId)).isEqualTo(1);
        assertThat(auditCount("USER_STATUS_CHANGED", userId)).isEqualTo(1);
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        createViewer(adminToken, "duplicate@werkpilot.local");

        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "DUPLICATE@werkpilot.local",
                                  "displayName": "Duplicate User",
                                  "temporaryPassword": "Duplicate-Change-Me-2026",
                                  "roles": ["VIEWER"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void lastActiveAdminCannotBeDemotedDisabledOrDeleted() throws Exception {
        MvcResult loginResult = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String adminToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
        String adminId = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.profile.id");

        mockMvc.perform(put("/users/{id}", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "WerkPilot Admin",
                                  "active": true,
                                  "roles": ["VIEWER"]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(patch("/users/{id}/status", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(delete("/users/{id}", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void deleteSoftDisablesNonLastAdminUser() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        String userId = createViewer(adminToken, "delete-me@werkpilot.local");

        mockMvc.perform(delete("/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    private String createViewer(String adminToken, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "Viewer User",
                                  "temporaryPassword": "Viewer-Change-Me-2026",
                                  "roles": ["VIEWER"]
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
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

    private long auditCount(String eventType, String targetUserId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from audit_event where event_type = ? and target_user_id = ?::uuid",
                Long.class,
                eventType,
                targetUserId);
        return count == null ? 0 : count;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
