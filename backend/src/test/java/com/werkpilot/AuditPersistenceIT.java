package com.werkpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
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
class AuditPersistenceIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditEventPort auditEventPort;

    @BeforeEach
    void clearAuditEvents() {
        jdbcTemplate.update("delete from audit_event");
    }

    @Test
    void appendedAuditEventsArePersistedAndReturnedNewestFirstWithPagination() throws Exception {
        MvcResult loginResult = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String adminToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
        String adminId = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.profile.id");

        auditEventPort.append(AuditEventType.USER_STATUS_CHANGED, UUID.fromString(adminId), UUID.fromString(adminId), "older-event", "trace-older");
        auditEventPort.append(AuditEventType.THRESHOLD_DELEGATION_CHANGED, UUID.fromString(adminId), null, "newer-event", "trace-newer");
        setOccurredAt("older-event", Instant.parse("2026-07-08T08:00:00Z"));
        setOccurredAt("newer-event", Instant.parse("2026-07-08T09:00:00Z"));

        mockMvc.perform(get("/audit-events")
                        .param("page", "0")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].eventType").value("THRESHOLD_DELEGATION_CHANGED"))
                .andExpect(jsonPath("$.items[0].actorUserId").value(adminId))
                .andExpect(jsonPath("$.items[0].details").value("newer-event"))
                .andExpect(jsonPath("$.items[0].traceId").value("trace-newer"));

        mockMvc.perform(get("/audit-events")
                        .param("page", "1")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventType").value("USER_STATUS_CHANGED"))
                .andExpect(jsonPath("$.items[0].targetUserId").value(adminId));
    }

    @Test
    void auditQueryFiltersByEventTypeActorTargetAndOccurrenceWindow() throws Exception {
        MvcResult loginResult = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String adminToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");
        String adminId = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.profile.id");
        String targetUserId = createViewer(adminToken);
        jdbcTemplate.update("delete from audit_event");

        auditEventPort.append(
                AuditEventType.USER_ROLE_CHANGED,
                UUID.fromString(adminId),
                UUID.fromString(targetUserId),
                "role-change-match",
                "trace-role");
        auditEventPort.append(
                AuditEventType.PASSWORD_RESET_REQUESTED,
                UUID.fromString(adminId),
                UUID.fromString(targetUserId),
                "reset-ignore",
                "trace-reset");
        setOccurredAt("role-change-match", Instant.parse("2026-07-08T10:00:00Z"));
        setOccurredAt("reset-ignore", Instant.parse("2026-07-08T12:00:00Z"));

        mockMvc.perform(get("/audit-events")
                        .param("eventType", "USER_ROLE_CHANGED")
                        .param("actorUserId", adminId)
                        .param("targetUserId", targetUserId)
                        .param("from", "2026-07-08T09:00:00Z")
                        .param("to", "2026-07-08T11:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].eventType").value("USER_ROLE_CHANGED"))
                .andExpect(jsonPath("$.items[0].actorUserId").value(adminId))
                .andExpect(jsonPath("$.items[0].targetUserId").value(targetUserId))
                .andExpect(jsonPath("$.items[0].details").value("role-change-match"));
    }

    @Test
    void unsupportedEventTypeAndInvalidWindowAreRejected() throws Exception {
        String adminToken = JsonPath.read(login(ADMIN_EMAIL, ADMIN_PASSWORD).getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/audit-events")
                        .param("eventType", "NO_SUCH_EVENT")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/audit-events")
                        .param("from", "2026-07-08T11:00:00Z")
                        .param("to", "2026-07-08T10:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    private String createViewer(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "audit.viewer.%s@werkpilot.local",
                                  "displayName": "Audit Viewer",
                                  "temporaryPassword": "Viewer-Change-Me-2026",
                                  "roles": ["VIEWER"]
                                }
                                """.formatted(suffix().toLowerCase())))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void setOccurredAt(String details, Instant occurredAt) {
        jdbcTemplate.update("update audit_event set occurred_at = ? where details = ?", Timestamp.from(occurredAt), details);
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
