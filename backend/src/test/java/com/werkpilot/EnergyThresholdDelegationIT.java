package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.energy.application.EnergyThresholdAuthorizationService;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import java.util.List;
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
class EnergyThresholdDelegationIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EnergyThresholdAuthorizationService authorizationService;

    @BeforeEach
    void resetDelegation() {
        jdbcTemplate.update("update system_settings set energy_threshold_delegation_enabled = false, updated_by_user_id = null, updated_at = now() where id = 1");
        jdbcTemplate.update("delete from audit_event where event_type = 'THRESHOLD_DELEGATION_CHANGED'");
    }

    @Test
    void delegationDefaultsOffAndIsReadableByAuthenticatedUsers() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        String energyEmail = "energy.default.%s@werkpilot.local".formatted(suffix().toLowerCase());
        createUser(adminToken, energyEmail, "ENERGY_MANAGER");
        String energyToken = loginAccessToken(energyEmail, "User-Change-Me-2026");

        mockMvc.perform(get("/settings/global")
                        .header(HttpHeaders.AUTHORIZATION, bearer(energyToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyThresholdDelegationEnabled").value(false));

        assertThatThrownBy(() -> authorizationService.assertCanWriteEnergyThreshold(
                new AuthenticatedPrincipal(UUID.randomUUID(), energyEmail, List.of("ENERGY_MANAGER"))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Energy threshold write access is not delegated.");
    }

    @Test
    void onlyAdminTogglesDelegationAndChangeIsAudited() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        MvcResult loginResult = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String adminId = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.profile.id");
        String viewerEmail = "viewer.settings.%s@werkpilot.local".formatted(suffix().toLowerCase());
        createUser(adminToken, viewerEmail, "VIEWER");
        String viewerToken = loginAccessToken(viewerEmail, "User-Change-Me-2026");

        mockMvc.perform(put("/settings/global/energy-threshold-delegation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(viewerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/settings/global/energy-threshold-delegation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyThresholdDelegationEnabled").value(true))
                .andExpect(jsonPath("$.updatedByUserId").value(adminId));

        Long auditCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from audit_event
                        where event_type = 'THRESHOLD_DELEGATION_CHANGED'
                          and actor_user_id = ?::uuid
                          and details = 'from=false;to=true'
                        """,
                Long.class,
                adminId);
        org.assertj.core.api.Assertions.assertThat(auditCount).isEqualTo(1);
    }

    @Test
    void adminAlwaysWritesThresholdsAndEnergyManagerRequiresDelegationOn() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        String energyEmail = "energy.guard.%s@werkpilot.local".formatted(suffix().toLowerCase());
        createUser(adminToken, energyEmail, "ENERGY_MANAGER");
        AuthenticatedPrincipal admin = new AuthenticatedPrincipal(UUID.randomUUID(), ADMIN_EMAIL, List.of("ADMIN"));
        AuthenticatedPrincipal energyManager = new AuthenticatedPrincipal(UUID.randomUUID(), energyEmail, List.of("ENERGY_MANAGER"));
        AuthenticatedPrincipal viewer = new AuthenticatedPrincipal(UUID.randomUUID(), "viewer@werkpilot.local", List.of("VIEWER"));

        assertThatCode(() -> authorizationService.assertCanWriteEnergyThreshold(admin))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> authorizationService.assertCanWriteEnergyThreshold(energyManager))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> authorizationService.assertCanWriteEnergyThreshold(viewer))
                .isInstanceOf(ApiException.class);

        mockMvc.perform(put("/settings/global/energy-threshold-delegation")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true}"))
                .andExpect(status().isOk());

        assertThatCode(() -> authorizationService.assertCanWriteEnergyThreshold(energyManager))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> authorizationService.assertCanWriteEnergyThreshold(viewer))
                .isInstanceOf(ApiException.class);
    }

    private void createUser(String adminToken, String email, String role) throws Exception {
        mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "Delegation User",
                                  "temporaryPassword": "User-Change-Me-2026",
                                  "roles": ["%s"]
                                }
                                """.formatted(email, role)))
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

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
