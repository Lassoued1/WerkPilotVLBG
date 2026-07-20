package com.werkpilot.maintenance.persistence;

import com.werkpilot.maintenance.application.MaintenanceTicket;
import com.werkpilot.maintenance.application.MaintenanceTicketComment;
import com.werkpilot.maintenance.application.MaintenanceTicketPort;
import com.werkpilot.maintenance.domain.TicketPriority;
import com.werkpilot.maintenance.domain.TicketStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMaintenanceTicketPersistenceAdapter implements MaintenanceTicketPort {
  private final JdbcTemplate jdbc;
  JdbcMaintenanceTicketPersistenceAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public Optional<MaintenanceTicket> findById(UUID id) { return jdbc.query("select * from maintenance_ticket where id=?", this::ticket, id).stream().findFirst(); }
  public List<MaintenanceTicket> list() { return jdbc.query("select * from maintenance_ticket order by updated_at desc", this::ticket); }
  public MaintenanceTicket create(MaintenanceTicket t) { jdbc.update("insert into maintenance_ticket (id,title,description,status,priority,factory_id,line_id,machine_id,anomaly_id,assignee_user_id,due_date,resolution_note,cancellation_reason,created_by_user_id,created_at,updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,now(),now())", t.id(),t.title(),t.description(),t.status().name(),t.priority().name(),t.factoryId(),t.lineId(),t.machineId(),t.anomalyId(),t.assigneeUserId(),t.dueDate(),t.resolutionNote(),t.cancellationReason(),t.createdByUserId()); return findById(t.id()).orElseThrow(); }
  public MaintenanceTicket update(MaintenanceTicket t) { jdbc.update("update maintenance_ticket set title=?,description=?,status=?,priority=?,factory_id=?,line_id=?,machine_id=?,anomaly_id=?,assignee_user_id=?,due_date=?,resolution_note=?,cancellation_reason=?,updated_at=now() where id=?", t.title(),t.description(),t.status().name(),t.priority().name(),t.factoryId(),t.lineId(),t.machineId(),t.anomalyId(),t.assigneeUserId(),t.dueDate(),t.resolutionNote(),t.cancellationReason(),t.id()); return findById(t.id()).orElseThrow(); }
  public MaintenanceTicketComment addComment(MaintenanceTicketComment c) { jdbc.update("insert into maintenance_ticket_comment (id,ticket_id,author_user_id,message,created_at) values (?,?,?,?,now())",c.id(),c.ticketId(),c.authorUserId(),c.message()); return jdbc.query("select * from maintenance_ticket_comment where id=?",this::comment,c.id()).getFirst(); }
  public List<MaintenanceTicketComment> comments(UUID id) { return jdbc.query("select * from maintenance_ticket_comment where ticket_id=? order by created_at",this::comment,id); }
  private MaintenanceTicket ticket(ResultSet r,int n) throws SQLException { return new MaintenanceTicket(r.getObject("id",UUID.class),r.getString("title"),r.getString("description"),TicketStatus.valueOf(r.getString("status")),TicketPriority.valueOf(r.getString("priority")),r.getObject("factory_id",UUID.class),r.getObject("line_id",UUID.class),r.getObject("machine_id",UUID.class),r.getObject("anomaly_id",UUID.class),r.getObject("assignee_user_id",UUID.class),r.getObject("due_date",java.time.LocalDate.class),r.getString("resolution_note"),r.getString("cancellation_reason"),r.getObject("created_by_user_id",UUID.class),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()); }
  private MaintenanceTicketComment comment(ResultSet r,int n) throws SQLException { return new MaintenanceTicketComment(r.getObject("id",UUID.class),r.getObject("ticket_id",UUID.class),r.getObject("author_user_id",UUID.class),r.getString("message"),r.getTimestamp("created_at").toInstant()); }
}
