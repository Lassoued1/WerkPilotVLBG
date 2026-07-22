package com.werkpilot.maintenance.persistence;

import com.werkpilot.maintenance.application.MaintenanceTicket;
import com.werkpilot.maintenance.application.MaintenanceTicketComment;
import com.werkpilot.maintenance.application.MaintenanceTicketPort;
import com.werkpilot.maintenance.application.TicketSearchCriteria;
import com.werkpilot.maintenance.domain.TicketPriority;
import com.werkpilot.maintenance.domain.TicketStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMaintenanceTicketPersistenceAdapter implements MaintenanceTicketPort {

    private final JdbcTemplate jdbc;

    JdbcMaintenanceTicketPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<MaintenanceTicket> findById(UUID id) {
        return jdbc.query("select * from maintenance_ticket where id = ?", this::ticket, id).stream().findFirst();
    }

    @Override
    public Optional<MaintenanceTicket> findByAnomalyId(UUID anomalyId) {
        return jdbc.query("select * from maintenance_ticket where anomaly_id = ?", this::ticket, anomalyId).stream().findFirst();
    }

    @Override
    public List<MaintenanceTicket> list(TicketSearchCriteria criteria, UUID preferredAssigneeId) {
        List<String> clauses = new ArrayList<>();
        List<Object> arguments = new ArrayList<>();
        add(clauses, arguments, "status", criteria.status() == null ? null : criteria.status().name());
        add(clauses, arguments, "priority", criteria.priority() == null ? null : criteria.priority().name());
        add(clauses, arguments, "assignee_user_id", criteria.assigneeUserId());
        add(clauses, arguments, "machine_id", criteria.machineId());

        String where = clauses.isEmpty() ? "" : " where " + String.join(" and ", clauses);
        List<Object> orderArguments = new ArrayList<>();
        String orderBy;
        if (preferredAssigneeId == null) {
            orderBy = " order by updated_at desc";
        } else {
            orderBy = " order by case when assignee_user_id = ? and status in ('OPEN', 'IN_PROGRESS') then 0 else 1 end, updated_at desc";
            orderArguments.add(preferredAssigneeId);
        }
        arguments.addAll(orderArguments);
        return jdbc.query("select * from maintenance_ticket" + where + orderBy, this::ticket, arguments.toArray());
    }

    @Override
    public List<MaintenanceTicket> findByMachineAndIssueCategoryCreatedSince(UUID machineId, String issueCategory, Instant createdAfter) {
        return jdbc.query(
                "select * from maintenance_ticket where machine_id = ? and issue_category = ? and created_at >= ? order by created_at",
                this::ticket,
                machineId,
                issueCategory,
                Timestamp.from(createdAfter));
    }

    @Override
    public MaintenanceTicket create(MaintenanceTicket ticket) {
        jdbc.update(
                """
                        insert into maintenance_ticket
                        (id, title, description, issue_category, status, priority, factory_id, line_id, machine_id,
                         anomaly_id, assignee_user_id, due_date, resolution_note, cancellation_reason,
                         created_by_user_id, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                        """,
                ticket.id(),
                ticket.title(),
                ticket.description(),
                ticket.issueCategory(),
                ticket.status().name(),
                ticket.priority().name(),
                ticket.factoryId(),
                ticket.lineId(),
                ticket.machineId(),
                ticket.anomalyId(),
                ticket.assigneeUserId(),
                ticket.dueDate(),
                ticket.resolutionNote(),
                ticket.cancellationReason(),
                ticket.createdByUserId());
        return findById(ticket.id()).orElseThrow();
    }

    @Override
    public MaintenanceTicket update(MaintenanceTicket ticket) {
        jdbc.update(
                """
                        update maintenance_ticket
                        set title = ?, description = ?, issue_category = ?, status = ?, priority = ?, factory_id = ?,
                            line_id = ?, machine_id = ?, anomaly_id = ?, assignee_user_id = ?, due_date = ?,
                            resolution_note = ?, cancellation_reason = ?, updated_at = now()
                        where id = ?
                        """,
                ticket.title(),
                ticket.description(),
                ticket.issueCategory(),
                ticket.status().name(),
                ticket.priority().name(),
                ticket.factoryId(),
                ticket.lineId(),
                ticket.machineId(),
                ticket.anomalyId(),
                ticket.assigneeUserId(),
                ticket.dueDate(),
                ticket.resolutionNote(),
                ticket.cancellationReason(),
                ticket.id());
        return findById(ticket.id()).orElseThrow();
    }

    @Override
    public MaintenanceTicketComment addComment(MaintenanceTicketComment comment) {
        jdbc.update(
                "insert into maintenance_ticket_comment (id, ticket_id, author_user_id, message, created_at) values (?, ?, ?, ?, now())",
                comment.id(),
                comment.ticketId(),
                comment.authorUserId(),
                comment.message());
        return jdbc.query("select * from maintenance_ticket_comment where id = ?", this::comment, comment.id()).getFirst();
    }

    @Override
    public List<MaintenanceTicketComment> comments(UUID ticketId) {
        return jdbc.query("select * from maintenance_ticket_comment where ticket_id = ? order by created_at", this::comment, ticketId);
    }

    private static void add(List<String> clauses, List<Object> arguments, String column, Object value) {
        if (value != null) {
            clauses.add(column + " = ?");
            arguments.add(value);
        }
    }

    private MaintenanceTicket ticket(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MaintenanceTicket(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("issue_category"),
                TicketStatus.valueOf(resultSet.getString("status")),
                TicketPriority.valueOf(resultSet.getString("priority")),
                resultSet.getObject("factory_id", UUID.class),
                resultSet.getObject("line_id", UUID.class),
                resultSet.getObject("machine_id", UUID.class),
                resultSet.getObject("anomaly_id", UUID.class),
                resultSet.getObject("assignee_user_id", UUID.class),
                resultSet.getObject("due_date", java.time.LocalDate.class),
                resultSet.getString("resolution_note"),
                resultSet.getString("cancellation_reason"),
                resultSet.getObject("created_by_user_id", UUID.class),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }

    private MaintenanceTicketComment comment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new MaintenanceTicketComment(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("ticket_id", UUID.class),
                resultSet.getObject("author_user_id", UUID.class),
                resultSet.getString("message"),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
