package com.werkpilot.audit.persistence;

import com.werkpilot.audit.application.port.AuditEvent;
import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.application.port.AuditEventSearchCriteria;
import com.werkpilot.audit.domain.AuditEventType;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditEventPersistenceAdapter implements AuditEventPort {

    private final AuditEventRepository repository;
    private final Clock clock;

    public AuditEventPersistenceAdapter(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void append(AuditEventType eventType, UUID actorUserId, UUID targetUserId, String details) {
        append(eventType, actorUserId, targetUserId, details, null);
    }

    @Override
    @Transactional
    public void append(AuditEventType eventType, UUID actorUserId, UUID targetUserId, String details, String traceId) {
        repository.save(new AuditEventEntity(
                UUID.randomUUID(),
                eventType,
                actorUserId,
                targetUserId,
                clock.instant(),
                details,
                traceId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEvent> search(AuditEventSearchCriteria criteria, Pageable pageable) {
        return repository.findAll((root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.eventType() != null) {
                predicates.add(builder.equal(root.get("eventType"), criteria.eventType()));
            }
            if (criteria.actorUserId() != null) {
                predicates.add(builder.equal(root.get("actorUserId"), criteria.actorUserId()));
            }
            if (criteria.targetUserId() != null) {
                predicates.add(builder.equal(root.get("targetUserId"), criteria.targetUserId()));
            }
            if (criteria.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), criteria.from()));
            }
            if (criteria.to() != null) {
                predicates.add(builder.lessThan(root.get("occurredAt"), criteria.to()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::toRecord);
    }

    private AuditEvent toRecord(AuditEventEntity entity) {
        return new AuditEvent(
                entity.getId(),
                entity.getEventType().name(),
                entity.getActorUserId(),
                entity.getTargetUserId(),
                entity.getOccurredAt(),
                entity.getDetails(),
                entity.getTraceId());
    }
}
