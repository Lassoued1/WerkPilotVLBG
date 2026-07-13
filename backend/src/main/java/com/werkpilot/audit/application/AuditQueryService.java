package com.werkpilot.audit.application;

import com.werkpilot.audit.application.port.AuditEvent;
import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.application.port.AuditEventSearchCriteria;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private final AuditEventPort auditEventPort;

    public AuditQueryService(AuditEventPort auditEventPort) {
        this.auditEventPort = auditEventPort;
    }

    @Transactional(readOnly = true)
    public AuditEventPage search(
            String eventType,
            UUID actorUserId,
            UUID targetUserId,
            Instant from,
            Instant to,
            int page,
            int size) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "Audit event 'from' must be before 'to'.");
        }

        AuditEventSearchCriteria criteria = new AuditEventSearchCriteria(
                parseEventType(eventType),
                actorUserId,
                targetUserId,
                from,
                to);
        Page<AuditEvent> events = auditEventPort.search(
                criteria,
                PageRequest.of(page, Math.min(size, 100), Sort.by("occurredAt").descending()));
        return new AuditEventPage(
                events.getContent(),
                events.getNumber(),
                events.getSize(),
                events.getTotalElements(),
                events.getTotalPages());
    }

    private static AuditEventType parseEventType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AuditEventType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.VALIDATION_FAILED,
                    "Audit event type is not supported.");
        }
    }

    public record AuditEventPage(
            java.util.List<AuditEvent> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {

        public AuditEventPage {
            items = java.util.List.copyOf(items);
        }
    }
}
