package com.werkpilot.audit.api;

import com.werkpilot.audit.application.AuditQueryService;
import com.werkpilot.audit.application.AuditQueryService.AuditEventPage;
import com.werkpilot.audit.application.port.AuditEvent;
import com.werkpilot.shared.api.PageResponse;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditEventController {

    private final AuditQueryService auditQueryService;

    public AuditEventController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/audit-events")
    PageResponse<AuditEventResponse> searchAuditEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) UUID targetUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        AuditEventPage events = auditQueryService.search(eventType, actorUserId, targetUserId, from, to, page, size);
        return new PageResponse<>(
                events.items().stream().map(this::response).toList(),
                events.page(),
                events.size(),
                events.totalElements(),
                events.totalPages());
    }

    private AuditEventResponse response(AuditEvent event) {
        return new AuditEventResponse(
                event.id(),
                event.eventType(),
                event.actorUserId(),
                event.targetUserId(),
                event.occurredAt(),
                event.details(),
                event.traceId());
    }

    public record AuditEventResponse(
            UUID id,
            String eventType,
            UUID actorUserId,
            UUID targetUserId,
            Instant occurredAt,
            String details,
            String traceId) {
    }
}
