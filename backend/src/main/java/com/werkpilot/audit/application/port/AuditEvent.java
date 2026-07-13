package com.werkpilot.audit.application.port;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        String eventType,
        UUID actorUserId,
        UUID targetUserId,
        Instant occurredAt,
        String details,
        String traceId) {
}
