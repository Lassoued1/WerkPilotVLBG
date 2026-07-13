package com.werkpilot.audit.application.port;

import com.werkpilot.audit.domain.AuditEventType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditEventPort {

    void append(AuditEventType eventType, UUID actorUserId, UUID targetUserId, String details);

    void append(AuditEventType eventType, UUID actorUserId, UUID targetUserId, String details, String traceId);

    Page<AuditEvent> search(AuditEventSearchCriteria criteria, Pageable pageable);
}
