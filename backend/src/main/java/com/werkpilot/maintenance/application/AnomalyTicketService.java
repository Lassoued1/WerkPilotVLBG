package com.werkpilot.maintenance.application;

import com.werkpilot.analytics.application.AnomalyPort;
import com.werkpilot.analytics.application.AnomalyRecord;
import com.werkpilot.analytics.domain.AnomalyStatus;
import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnomalyTicketService {

    private final AnomalyPort anomalyPort;
    private final MaintenanceTicketService ticketService;
    private final AuditEventPort auditEventPort;

    AnomalyTicketService(
            AnomalyPort anomalyPort,
            MaintenanceTicketService ticketService,
            AuditEventPort auditEventPort) {
        this.anomalyPort = anomalyPort;
        this.ticketService = ticketService;
        this.auditEventPort = auditEventPort;
    }

    @Transactional
    public MaintenanceTicket createFromAnomaly(AuthenticatedPrincipal actor, UUID anomalyId, CreateFromAnomalyCommand command) {
        AnomalyRecord anomaly = anomalyPort.findById(anomalyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Anomaly was not found."));
        if (anomaly.status() == AnomalyStatus.SUPERSEDED) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE_VIOLATION, "Superseded anomalies cannot be converted to tickets.");
        }
        if (ticketService.findByAnomalyId(anomalyId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE_VIOLATION, "A ticket is already linked to this anomaly.");
        }

        MaintenanceTicket ticket = ticketService.create(actor, new MaintenanceTicketService.TicketCommand(
                command.title(),
                anomaly.explanation(),
                command.issueCategory(),
                command.priority(),
                anomaly.factoryId(),
                anomaly.lineId(),
                anomaly.machineId(),
                anomaly.id(),
                command.assigneeUserId(),
                command.dueDate()));

        AnomalyRecord updated = anomalyPort.updateStatus(anomalyId, AnomalyStatus.LINKED_TO_TICKET);
        auditEventPort.append(
                AuditEventType.ANOMALY_STATUS_CHANGED,
                actor.userId(),
                null,
                "anomalyId=%s; oldStatus=%s; newStatus=%s; ticketId=%s"
                        .formatted(anomalyId, anomaly.status(), updated.status(), ticket.id()));
        return ticket;
    }

    public record CreateFromAnomalyCommand(
            String title,
            String issueCategory,
            String priority,
            UUID assigneeUserId,
            LocalDate dueDate) {
    }
}
