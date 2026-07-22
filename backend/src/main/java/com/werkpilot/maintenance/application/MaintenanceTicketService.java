package com.werkpilot.maintenance.application;

import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.maintenance.domain.TicketPriority;
import com.werkpilot.maintenance.domain.TicketStatus;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceTicketService {

    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");

    private final MaintenanceTicketPort port;
    private final AuditEventPort audit;
    private final Clock clock;
    private final RecurringTicketPatternService recurringTicketPatternService;

    @Autowired
    public MaintenanceTicketService(
            MaintenanceTicketPort port,
            AuditEventPort audit,
            RecurringTicketPatternService recurringTicketPatternService) {
        this(port, audit, Clock.systemUTC(), recurringTicketPatternService);
    }

    MaintenanceTicketService(
            MaintenanceTicketPort port,
            AuditEventPort audit,
            Clock clock,
            RecurringTicketPatternService recurringTicketPatternService) {
        this.port = port;
        this.audit = audit;
        this.clock = clock;
        this.recurringTicketPatternService = recurringTicketPatternService;
    }

    public List<MaintenanceTicket> list(
            AuthenticatedPrincipal actor,
            String requestedStatus,
            String requestedPriority,
            UUID assigneeUserId,
            UUID machineId) {
        return port.list(
                new TicketSearchCriteria(status(requestedStatus), priority(requestedPriority), assigneeUserId, machineId),
                actor.userId());
    }

    public MaintenanceTicket get(UUID id) {
        return port.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Ticket was not found."));
    }

    public Optional<MaintenanceTicket> findByAnomalyId(UUID anomalyId) {
        return port.findByAnomalyId(anomalyId);
    }

    public List<MaintenanceTicketComment> comments(UUID id) {
        get(id);
        return port.comments(id);
    }

    @Transactional
    public MaintenanceTicket create(AuthenticatedPrincipal actor, TicketCommand command) {
        requireManager(actor);
        if (command.title() == null || command.title().isBlank()) {
            throw invalid("title is required");
        }
        MaintenanceTicket created = port.create(new MaintenanceTicket(
                UUID.randomUUID(),
                command.title().trim(),
                trimToNull(command.description()),
                trimToNull(command.issueCategory()),
                TicketStatus.OPEN,
                priorityOrDefault(command.priority()),
                command.factoryId(),
                command.lineId(),
                command.machineId(),
                command.anomalyId(),
                command.assigneeUserId(),
                command.dueDate(),
                null,
                null,
                actor.userId(),
                null,
                null));
        recurringTicketPatternService.detectIfRecurring(created);
        return created;
    }

    @Transactional
    public MaintenanceTicket changeStatus(AuthenticatedPrincipal actor, UUID id, String requestedStatus, String note) {
        MaintenanceTicket ticket = get(id);
        authorizeActor(actor, ticket);
        TicketStatus next = requiredStatus(requestedStatus);
        if (!allowed(ticket.status(), next)) {
            throw businessRule("Unsupported ticket transition");
        }
        if (next == TicketStatus.RESOLVED && isBlank(note)) {
            throw invalid("resolution note is required");
        }
        if (next == TicketStatus.CANCELLED && isBlank(note)) {
            throw invalid("cancellation reason is required");
        }
        MaintenanceTicket updated = port.update(new MaintenanceTicket(
                ticket.id(),
                ticket.title(),
                ticket.description(),
                ticket.issueCategory(),
                next,
                ticket.priority(),
                ticket.factoryId(),
                ticket.lineId(),
                ticket.machineId(),
                ticket.anomalyId(),
                ticket.assigneeUserId(),
                ticket.dueDate(),
                next == TicketStatus.RESOLVED ? note.trim() : ticket.resolutionNote(),
                next == TicketStatus.CANCELLED ? note.trim() : ticket.cancellationReason(),
                ticket.createdByUserId(),
                ticket.createdAt(),
                ticket.updatedAt()));
        audit.append(
                AuditEventType.TICKET_STATUS_CHANGED,
                actor.userId(),
                null,
                "ticketId=" + id + ";oldStatus=" + ticket.status() + ";newStatus=" + next);
        return updated;
    }

    @Transactional
    public MaintenanceTicketComment addComment(AuthenticatedPrincipal actor, UUID id, String message) {
        MaintenanceTicket ticket = get(id);
        authorizeActor(actor, ticket);
        if (isBlank(message)) {
            throw invalid("message is required");
        }
        MaintenanceTicketComment comment = port.addComment(new MaintenanceTicketComment(
                UUID.randomUUID(), id, actor.userId(), message.trim(), null));
        audit.append(AuditEventType.TICKET_COMMENT_ADDED, actor.userId(), null, "ticketId=" + id + ";commentId=" + comment.id());
        return comment;
    }

    public boolean overdue(MaintenanceTicket ticket) {
        return ticket.dueDate() != null
                && ticket.dueDate().isBefore(LocalDate.now(clock.withZone(VIENNA)))
                && (ticket.status() == TicketStatus.OPEN || ticket.status() == TicketStatus.IN_PROGRESS);
    }

    private static boolean allowed(TicketStatus from, TicketStatus to) {
        return (from == TicketStatus.OPEN && (to == TicketStatus.IN_PROGRESS || to == TicketStatus.CANCELLED))
                || (from == TicketStatus.IN_PROGRESS && (to == TicketStatus.RESOLVED || to == TicketStatus.CANCELLED));
    }

    private static TicketStatus requiredStatus(String value) {
        TicketStatus status = status(value);
        if (status == null) {
            throw invalid("ticket status is required");
        }
        return status;
    }

    private static TicketStatus status(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TicketStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid("Unsupported ticket status");
        }
    }

    private static TicketPriority priority(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return TicketPriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid("Unsupported priority");
        }
    }

    private static TicketPriority priorityOrDefault(String value) {
        TicketPriority priority = priority(value);
        return priority == null ? TicketPriority.MEDIUM : priority;
    }

    private static void requireManager(AuthenticatedPrincipal actor) {
        if (!actor.roles().contains("ADMIN")
                && !actor.roles().contains("PRODUCTION_MANAGER")
                && !actor.roles().contains("ENERGY_MANAGER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Only managers can create tickets.");
        }
    }

    private static void authorizeActor(AuthenticatedPrincipal actor, MaintenanceTicket ticket) {
        if (actor.roles().contains("ADMIN")
                || actor.roles().contains("PRODUCTION_MANAGER")
                || actor.roles().contains("ENERGY_MANAGER")
                || actor.userId().equals(ticket.assigneeUserId())) {
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Ticket access denied.");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message);
    }

    private static ApiException businessRule(String message) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public record TicketCommand(
            String title,
            String description,
            String issueCategory,
            String priority,
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID anomalyId,
            UUID assigneeUserId,
            LocalDate dueDate) {
    }
}
