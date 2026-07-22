package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class MaintenanceTicketLifecycleIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";
    private static final String USER_PASSWORD = "User-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetTicketState() {
        jdbcTemplate.update("delete from maintenance_ticket_comment");
        jdbcTemplate.update("delete from maintenance_ticket");
        jdbcTemplate.update("delete from audit_event where event_type = 'TICKET_STATUS_CHANGED'");
    }

    @Test
    void adminMovesTicketFromOpenToInProgressToResolvedAndEachTransitionIsAudited() throws Exception {
        MvcResult adminLogin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String adminToken = JsonPath.read(adminLogin.getResponse().getContentAsString(), "$.accessToken");
        String adminId = JsonPath.read(adminLogin.getResponse().getContentAsString(), "$.profile.id");
        String ticketId = createTicket(adminToken, null);

        assertThat(ticketStatusChangedAuditCount(ticketId)).isZero();

        changeStatus(adminToken, ticketId, "IN_PROGRESS", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        assertThat(ticketStatusChangedAuditCount(ticketId)).isEqualTo(1);

        changeStatus(adminToken, ticketId, "RESOLVED", "Replaced worn bearing.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolutionNote").value("Replaced worn bearing."));
        assertThat(ticketStatusChangedAuditCount(ticketId)).isEqualTo(2);
        assertThat(ticketStatusChangedAuditCountForActor(ticketId, adminId)).isEqualTo(2);
    }

    @Test
    void resolvedRequiresNonblankResolutionNote() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        String ticketId = createTicket(adminToken, null);

        changeStatus(adminToken, ticketId, "IN_PROGRESS", null).andExpect(status().isOk());

        changeStatus(adminToken, ticketId, "RESOLVED", "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        changeStatus(adminToken, ticketId, "RESOLVED", "Fixed and tested.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void cancelledRequiresNonblankCancellationReasonFromOpenAndInProgress() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        String openTicketId = createTicket(adminToken, null);

        changeStatus(adminToken, openTicketId, "CANCELLED", " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        changeStatus(adminToken, openTicketId, "CANCELLED", "Duplicate ticket.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Duplicate ticket."));

        String inProgressTicketId = createTicket(adminToken, null);
        changeStatus(adminToken, inProgressTicketId, "IN_PROGRESS", null).andExpect(status().isOk());

        changeStatus(adminToken, inProgressTicketId, "CANCELLED", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        changeStatus(adminToken, inProgressTicketId, "CANCELLED", "Machine was decommissioned.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Machine was decommissioned."));

        assertThat(ticketStatusChangedAuditCount(openTicketId)).isEqualTo(1);
        assertThat(ticketStatusChangedAuditCount(inProgressTicketId)).isEqualTo(2);
    }

    @Test
    void invalidTransitionsAreRejected() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        String openTicketId = createTicket(adminToken, null);
        changeStatus(adminToken, openTicketId, "RESOLVED", "Cannot skip in-progress.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        String resolvedTicketId = createTicket(adminToken, null);
        changeStatus(adminToken, resolvedTicketId, "IN_PROGRESS", null).andExpect(status().isOk());
        changeStatus(adminToken, resolvedTicketId, "RESOLVED", "Done.").andExpect(status().isOk());

        changeStatus(adminToken, resolvedTicketId, "OPEN", null).andExpect(status().isConflict());
        changeStatus(adminToken, resolvedTicketId, "IN_PROGRESS", null).andExpect(status().isConflict());
        changeStatus(adminToken, resolvedTicketId, "CANCELLED", "No longer needed.").andExpect(status().isConflict());

        String cancelledTicketId = createTicket(adminToken, null);
        changeStatus(adminToken, cancelledTicketId, "CANCELLED", "Duplicate.").andExpect(status().isOk());

        changeStatus(adminToken, cancelledTicketId, "OPEN", null).andExpect(status().isConflict());
        changeStatus(adminToken, cancelledTicketId, "IN_PROGRESS", null).andExpect(status().isConflict());
        changeStatus(adminToken, cancelledTicketId, "RESOLVED", "Done.").andExpect(status().isConflict());

        assertThat(ticketStatusChangedAuditCount(openTicketId)).isZero();
        assertThat(ticketStatusChangedAuditCount(resolvedTicketId)).isEqualTo(2);
        assertThat(ticketStatusChangedAuditCount(cancelledTicketId)).isEqualTo(1);
    }

    @Test
    void assignedTechnicianCanUpdateStatusAndAppendVisibleComment() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        CreatedUser technician = createUser(adminToken, "assigned-tech", "MAINTENANCE_TECHNICIAN");
        String ticketId = createTicket(adminToken, technician.id());

        changeStatus(technician.accessToken(), ticketId, "IN_PROGRESS", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/maintenance-tickets/{id}/comments", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(technician.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Checked vibration and ordered replacement part."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Checked vibration and ordered replacement part."))
                .andExpect(jsonPath("$.authorUserId").value(technician.id()));

        mockMvc.perform(get("/maintenance-tickets/{id}", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.id").value(ticketId))
                .andExpect(jsonPath("$.ticket.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].message").value("Checked vibration and ordered replacement part."))
                .andExpect(jsonPath("$.comments[0].authorUserId").value(technician.id()));

        assertThat(commentCount(ticketId)).isEqualTo(1);
        assertThat(ticketStatusChangedAuditCount(ticketId)).isEqualTo(1);
    }

    @Test
    void unassignedTechnicianReceivesForbidden() throws Exception {
        String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        CreatedUser assignedTechnician = createUser(adminToken, "assigned-deny-tech", "MAINTENANCE_TECHNICIAN");
        CreatedUser unassignedTechnician = createUser(adminToken, "unassigned-deny-tech", "MAINTENANCE_TECHNICIAN");
        String ticketId = createTicket(adminToken, assignedTechnician.id());

        changeStatus(unassignedTechnician.accessToken(), ticketId, "IN_PROGRESS", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        mockMvc.perform(post("/maintenance-tickets/{id}/comments", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(unassignedTechnician.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Trying to comment without assignment."}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        assertThat(ticketStatusChangedAuditCount(ticketId)).isZero();
        assertThat(commentCount(ticketId)).isZero();
    }

    private String createTicket(String adminToken, String assigneeUserId) throws Exception {
        String assigneeField = assigneeUserId == null
                ? "\"assigneeUserId\": null,"
                : "\"assigneeUserId\": \"%s\",".formatted(assigneeUserId);
        MvcResult result = mockMvc.perform(post("/maintenance-tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hydraulic press inspection %s",
                                  "description": "Inspect reported pressure instability.",
                                  "priority": "HIGH",
                                  %s
                                  "dueDate": "2026-07-19"
                                }
                                """.formatted(suffix(), assigneeField)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private CreatedUser createUser(String adminToken, String prefix, String role) throws Exception {
        String email = "%s.%s@werkpilot.local".formatted(prefix, suffix().toLowerCase());
        MvcResult result = mockMvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "displayName": "Maintenance Technician",
                                  "temporaryPassword": "%s",
                                  "roles": ["%s"]
                                }
                                """.formatted(email, USER_PASSWORD, role)))
                .andExpect(status().isCreated())
                .andReturn();
        return new CreatedUser(
                JsonPath.read(result.getResponse().getContentAsString(), "$.id"),
                loginAccessToken(email, USER_PASSWORD));
    }

    private org.springframework.test.web.servlet.ResultActions changeStatus(
            String accessToken,
            String ticketId,
            String status,
            String note) throws Exception {
        String noteJson = note == null ? "null" : "\"" + note.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        return mockMvc.perform(patch("/maintenance-tickets/{id}/status", ticketId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "status": "%s",
                          "note": %s
                        }
                        """.formatted(status, noteJson)));
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

    private long ticketStatusChangedAuditCount(String ticketId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from audit_event where event_type = 'TICKET_STATUS_CHANGED' and details like ?",
                Long.class,
                "%ticketId=" + ticketId + "%");
        return count == null ? 0 : count;
    }

    private long ticketStatusChangedAuditCountForActor(String ticketId, String actorUserId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from audit_event
                        where event_type = 'TICKET_STATUS_CHANGED'
                          and actor_user_id = ?::uuid
                          and details like ?
                        """,
                Long.class,
                actorUserId,
                "%ticketId=" + ticketId + "%");
        return count == null ? 0 : count;
    }

    private long commentCount(String ticketId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from maintenance_ticket_comment where ticket_id = ?::uuid",
                Long.class,
                ticketId);
        return count == null ? 0 : count;
    }

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record CreatedUser(String id, String accessToken) {
    }
}
