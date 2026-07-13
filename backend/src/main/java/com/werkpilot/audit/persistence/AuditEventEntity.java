package com.werkpilot.audit.persistence;

import com.werkpilot.audit.domain.AuditEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEventEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AuditEventType eventType;

    private UUID actorUserId;

    private UUID targetUserId;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 1000)
    private String details;

    @Column(length = 80)
    private String traceId;

    protected AuditEventEntity() {
    }

    AuditEventEntity(UUID id, AuditEventType eventType, UUID actorUserId, UUID targetUserId, Instant occurredAt, String details) {
        this(id, eventType, actorUserId, targetUserId, occurredAt, details, null);
    }

    AuditEventEntity(UUID id, AuditEventType eventType, UUID actorUserId, UUID targetUserId, Instant occurredAt, String details, String traceId) {
        this.id = id;
        this.eventType = eventType;
        this.actorUserId = actorUserId;
        this.targetUserId = targetUserId;
        this.occurredAt = occurredAt;
        this.details = details;
        this.traceId = traceId;
    }

    UUID getId() {
        return id;
    }

    AuditEventType getEventType() {
        return eventType;
    }

    UUID getActorUserId() {
        return actorUserId;
    }

    UUID getTargetUserId() {
        return targetUserId;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    String getDetails() {
        return details;
    }

    String getTraceId() {
        return traceId;
    }
}
