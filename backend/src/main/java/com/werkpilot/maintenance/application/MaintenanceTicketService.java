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
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class MaintenanceTicketService {
  private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna"); private final MaintenanceTicketPort port; private final AuditEventPort audit; private final Clock clock;
  @Autowired
  public MaintenanceTicketService(MaintenanceTicketPort port, AuditEventPort audit) { this(port,audit,Clock.systemUTC()); }
  MaintenanceTicketService(MaintenanceTicketPort port, AuditEventPort audit, Clock clock) { this.port=port;this.audit=audit;this.clock=clock; }
  public List<MaintenanceTicket> list(){return port.list();} public MaintenanceTicket get(UUID id){return port.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,ErrorCode.RESOURCE_NOT_FOUND,"Ticket was not found."));} public List<MaintenanceTicketComment> comments(UUID id){get(id);return port.comments(id);}
  @Transactional public MaintenanceTicket create(AuthenticatedPrincipal actor, TicketCommand c){ requireManager(actor); if(c.title()==null||c.title().isBlank()) throw invalid("title is required"); MaintenanceTicket t=new MaintenanceTicket(UUID.randomUUID(),c.title().trim(),c.description(),TicketStatus.OPEN,priority(c.priority()),c.factoryId(),c.lineId(),c.machineId(),c.anomalyId(),c.assigneeUserId(),c.dueDate(),null,null,actor.userId(),null,null); return port.create(t); }
  @Transactional public MaintenanceTicket changeStatus(AuthenticatedPrincipal actor, UUID id, String requested, String note){ MaintenanceTicket t=get(id); authorizeActor(actor,t); TicketStatus next=status(requested); if(!allowed(t.status(),next)) throw invalid("Unsupported ticket transition"); if(next==TicketStatus.RESOLVED&&(note==null||note.isBlank())) throw invalid("resolution note is required"); if(next==TicketStatus.CANCELLED&&(note==null||note.isBlank())) throw invalid("cancellation reason is required"); MaintenanceTicket updated=new MaintenanceTicket(t.id(),t.title(),t.description(),next,t.priority(),t.factoryId(),t.lineId(),t.machineId(),t.anomalyId(),t.assigneeUserId(),t.dueDate(),next==TicketStatus.RESOLVED?note:t.resolutionNote(),next==TicketStatus.CANCELLED?note:t.cancellationReason(),t.createdByUserId(),t.createdAt(),t.updatedAt()); updated=port.update(updated); audit.append(AuditEventType.TICKET_STATUS_CHANGED,actor.userId(),null,"ticketId="+id+";oldStatus="+t.status()+";newStatus="+next); return updated; }
  @Transactional public MaintenanceTicketComment addComment(AuthenticatedPrincipal actor,UUID id,String message){MaintenanceTicket t=get(id);authorizeActor(actor,t);if(message==null||message.isBlank())throw invalid("message is required");return port.addComment(new MaintenanceTicketComment(UUID.randomUUID(),id,actor.userId(),message.trim(),null));}
  public boolean overdue(MaintenanceTicket t){return t.dueDate()!=null&&t.dueDate().isBefore(LocalDate.now(clock.withZone(VIENNA)))&&(t.status()==TicketStatus.OPEN||t.status()==TicketStatus.IN_PROGRESS);}
  private static boolean allowed(TicketStatus from,TicketStatus to){return (from==TicketStatus.OPEN&&(to==TicketStatus.IN_PROGRESS||to==TicketStatus.CANCELLED))||(from==TicketStatus.IN_PROGRESS&&(to==TicketStatus.RESOLVED||to==TicketStatus.CANCELLED));} private static TicketStatus status(String s){try{return TicketStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));}catch(Exception e){throw invalid("Unsupported ticket status");}} private static TicketPriority priority(String s){try{return s==null?TicketPriority.MEDIUM:TicketPriority.valueOf(s.trim().toUpperCase(Locale.ROOT));}catch(Exception e){throw invalid("Unsupported priority");}} private static void requireManager(AuthenticatedPrincipal a){if(!a.roles().contains("ADMIN")&&!a.roles().contains("PRODUCTION_MANAGER")&&!a.roles().contains("ENERGY_MANAGER"))throw new ApiException(HttpStatus.FORBIDDEN,ErrorCode.ACCESS_DENIED,"Only managers can create tickets.");} private static void authorizeActor(AuthenticatedPrincipal a,MaintenanceTicket t){if(a.roles().contains("ADMIN")||a.roles().contains("PRODUCTION_MANAGER")||a.roles().contains("ENERGY_MANAGER")||a.userId().equals(t.assigneeUserId()))return;throw new ApiException(HttpStatus.FORBIDDEN,ErrorCode.ACCESS_DENIED,"Ticket access denied.");} private static ApiException invalid(String m){return new ApiException(HttpStatus.BAD_REQUEST,ErrorCode.VALIDATION_FAILED,m);}
  public record TicketCommand(String title,String description,String priority,UUID factoryId,UUID lineId,UUID machineId,UUID anomalyId,UUID assigneeUserId,LocalDate dueDate){}
}
