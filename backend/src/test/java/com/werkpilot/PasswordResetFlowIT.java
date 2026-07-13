package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.identity.application.port.PasswordResetMailPort;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import jakarta.servlet.http.Cookie;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetFlowIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";
    private static final String REFRESH_COOKIE_NAME = "werkpilot_refresh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingPasswordResetMailPort mailPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearMail() {
        mailPort.clear();
    }

    @Test
    void publicResetRequestIsEnumerationSafe() throws Exception {
        mockMvc.perform(post("/auth/password-reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@werkpilot.local\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        assertThat(mailPort.messages()).isEmpty();
    }

    @Test
    void resetConfirmationChangesPasswordConsumesTokenAndRevokesSessions() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        createUser(adminToken, "reset.flow@werkpilot.local", "Old-Password-2026", "VIEWER");

        MvcResult oldLogin = login("reset.flow@werkpilot.local", "Old-Password-2026");
        String oldCsrf = JsonPath.read(oldLogin.getResponse().getContentAsString(), "$.csrfToken");
        Cookie oldRefreshCookie = refreshCookieFrom(oldLogin);

        mockMvc.perform(post("/auth/password-reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"RESET.FLOW@werkpilot.local\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        assertThat(mailPort.messages()).hasSize(1);
        RecordingPasswordResetMailPort.Message message = mailPort.messages().getFirst();
        assertThat(message.recipientEmail()).isEqualTo("reset.flow@werkpilot.local");
        assertThat(message.resetLink()).contains("/password-reset#token=");
        assertThat(message.resetLink()).doesNotContain("?token=");
        String token = tokenFrom(message.resetLink());

        mockMvc.perform(post("/auth/password-reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"New-Password-2026"}
                                """.formatted(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"reset.flow@werkpilot.local","password":"Old-Password-2026"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));

        login("reset.flow@werkpilot.local", "New-Password-2026");

        mockMvc.perform(post("/auth/refresh")
                        .cookie(oldRefreshCookie)
                        .header("X-WerkPilot-CSRF", oldCsrf))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_EXPIRED"));

        mockMvc.perform(post("/auth/password-reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"Another-Password-2026"}
                                """.formatted(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_EXPIRED"));

        String userId = userIdByEmail("reset.flow@werkpilot.local");
        assertThat(auditCount("PASSWORD_RESET_REQUESTED", userId)).isEqualTo(1);
        assertThat(auditCount("PASSWORD_RESET_COMPLETED", userId)).isEqualTo(1);
    }

    @Test
    void adminTriggerSendsResetMailWithoutRevealingToken() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        String userId = createUser(adminToken, "admin.reset@werkpilot.local", "Temporary-Password-2026", "VIEWER");

        mockMvc.perform(post("/users/{id}/password-reset", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isAccepted())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("token"))));

        assertThat(mailPort.messages()).hasSize(1);
        assertThat(mailPort.messages().getFirst().recipientEmail()).isEqualTo("admin.reset@werkpilot.local");
        assertThat(mailPort.messages().getFirst().resetLink()).contains("#token=");
    }

    private String createUser(String adminToken, String email, String password, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "Reset User",
                                  "temporaryPassword": "%s",
                                  "roles": ["%s"]
                                }
                                """.formatted(email, password, role)))
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

    private Cookie refreshCookieFrom(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String cookiePair = setCookie.split(";", 2)[0];
        String refreshToken = cookiePair.substring((REFRESH_COOKIE_NAME + "=").length());
        return new Cookie(REFRESH_COOKIE_NAME, refreshToken);
    }

    private String tokenFrom(String resetLink) {
        String token = resetLink.substring(resetLink.indexOf("#token=") + "#token=".length());
        return URLDecoder.decode(token, StandardCharsets.UTF_8);
    }

    private String userIdByEmail(String email) {
        return jdbcTemplate.queryForObject("select id::text from app_user where email = ?", String.class, email);
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

    @TestConfiguration
    static class PasswordResetTestConfiguration {

        @Bean
        @Primary
        RecordingPasswordResetMailPort recordingPasswordResetMailPort() {
            return new RecordingPasswordResetMailPort();
        }
    }

    static class RecordingPasswordResetMailPort implements PasswordResetMailPort {

        private final List<Message> messages = new ArrayList<>();

        @Override
        public void sendPasswordResetMail(String recipientEmail, String displayName, String resetLink) {
            messages.add(new Message(recipientEmail, displayName, resetLink));
        }

        List<Message> messages() {
            return messages;
        }

        void clear() {
            messages.clear();
        }

        record Message(String recipientEmail, String displayName, String resetLink) {
        }
    }
}
