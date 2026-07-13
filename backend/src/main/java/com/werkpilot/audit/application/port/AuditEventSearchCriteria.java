package com.werkpilot.audit.application.port;

import com.werkpilot.audit.domain.AuditEventType;
import java.time.Instant;
import java.util.UUID;

public record AuditEventSearchCriteria(
        AuditEventType eventType,
        UUID actorUserId,
        UUID targetUserId,
        Instant from,
        Instant to) {
}
