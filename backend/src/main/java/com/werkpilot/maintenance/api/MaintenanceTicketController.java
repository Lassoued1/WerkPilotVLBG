package com.werkpilot.maintenance.api;

import com.werkpilot.maintenance.application.MaintenanceTicket;
import com.werkpilot.maintenance.application.MaintenanceTicketComment;
import com.werkpilot.maintenance.application.MaintenanceTicketService;
import com.werkpilot.maintenance.application.AnomalyTicketService;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MaintenanceTicketController {

    private final MaintenanceTicketService service;
    private final AnomalyTicketService anomalyTicketService;

    MaintenanceTicketController(MaintenanceTicketService service, AnomalyTicketService anomalyTicketService) {
        this.service = service;
        this.anomalyTicketService = anomalyTicketService;
    }

    @GetMapping("/maintenance-tickets")
    List<TicketResponse> list(
            Authentication authentication,
            @RequestParam(required = false) String ticketStatus,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) UUID machineId) {
        return service.list(principal(authentication), ticketStatus, priority, assigneeId, machineId).stream()
                .map(this::response)
                .toList();
    }

    @GetMapping("/maintenance-tickets/{id}")
    TicketDetailResponse get(@PathVariable UUID id) {
        MaintenanceTicket ticket = service.get(id);
        return new TicketDetailResponse(response(ticket), service.comments(id));
    }

    @PostMapping("/maintenance-tickets")
    @ResponseStatus(HttpStatus.CREATED)
    TicketResponse create(Authentication authentication, @Valid @RequestBody TicketRequest request) {
        return response(service.create(principal(authentication), new MaintenanceTicketService.TicketCommand(
                request.title(),
                request.description(),
                request.issueCategory(),
                request.priority(),
                request.factoryId(),
                request.lineId(),
                request.machineId(),
                request.anomalyId(),
                request.assigneeUserId(),
                request.dueDate())));
    }

    @GetMapping("/anomalies/{id}/ticket")
    AnomalyTicketResponse ticketForAnomaly(@PathVariable UUID id) {
        MaintenanceTicket ticket = service.findByAnomalyId(id)
                .orElseThrow(() -> new com.werkpilot.shared.error.ApiException(
                        HttpStatus.NOT_FOUND,
                        com.werkpilot.shared.error.ErrorCode.RESOURCE_NOT_FOUND,
                        "No ticket is linked to this anomaly."));
        return new AnomalyTicketResponse(ticket.id(), ticket.title(), ticket.statusName(), ticket.priorityName());
    }

    @PostMapping("/anomalies/{id}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    AnomalyTicketResponse createFromAnomaly(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody CreateTicketFromAnomalyRequest request) {
        MaintenanceTicket ticket = anomalyTicketService.createFromAnomaly(
                principal(authentication),
                id,
                new AnomalyTicketService.CreateFromAnomalyCommand(
                        request.title(),
                        request.issueCategory(),
                        request.priority(),
                        request.assigneeUserId(),
                        request.dueDate()));
        return new AnomalyTicketResponse(ticket.id(), ticket.title(), ticket.statusName(), ticket.priorityName());
    }

    @PatchMapping("/maintenance-tickets/{id}/status")
    TicketResponse status(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody TicketStatusRequest request) {
        return response(service.changeStatus(principal(authentication), id, request.status(), request.note()));
    }

    @PostMapping("/maintenance-tickets/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    MaintenanceTicketComment comment(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody CommentRequest request) {
        return service.addComment(principal(authentication), id, request.message());
    }

    private static AuthenticatedPrincipal principal(Authentication authentication) {
        return (AuthenticatedPrincipal) authentication.getPrincipal();
    }

    private TicketResponse response(MaintenanceTicket ticket) {
        return new TicketResponse(
                ticket.id(),
                ticket.title(),
                ticket.description(),
                ticket.issueCategory(),
                ticket.statusName(),
                ticket.priorityName(),
                ticket.factoryId(),
                ticket.lineId(),
                ticket.machineId(),
                ticket.anomalyId(),
                ticket.assigneeUserId(),
                ticket.dueDate(),
                ticket.resolutionNote(),
                ticket.cancellationReason(),
                ticket.createdByUserId(),
                ticket.createdAt(),
                ticket.updatedAt(),
                service.overdue(ticket));
    }

    record TicketRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @Size(max = 100) String issueCategory,
            @Size(max = 20) String priority,
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID anomalyId,
            UUID assigneeUserId,
            LocalDate dueDate) {
    }

    record TicketStatusRequest(@NotBlank String status, @Size(max = 2000) String note) {
    }

    record CreateTicketFromAnomalyRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 100) String issueCategory,
            @Size(max = 20) String priority,
            UUID assigneeUserId,
            LocalDate dueDate) {
    }

    record CommentRequest(@NotBlank @Size(max = 2000) String message) {
    }

    record TicketResponse(
            UUID id,
            String title,
            String description,
            String issueCategory,
            String status,
            String priority,
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID anomalyId,
            UUID assigneeUserId,
            LocalDate dueDate,
            String resolutionNote,
            String cancellationReason,
            UUID createdByUserId,
            java.time.Instant createdAt,
            java.time.Instant updatedAt,
            boolean overdue) {
    }

    record TicketDetailResponse(TicketResponse ticket, List<MaintenanceTicketComment> comments) {
    }

    record AnomalyTicketResponse(UUID id, String title, String status, String priority) {
    }
}
