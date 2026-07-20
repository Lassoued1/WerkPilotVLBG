package com.werkpilot.maintenance.api;

import com.werkpilot.maintenance.application.MaintenanceTicket;
import com.werkpilot.maintenance.application.MaintenanceTicketComment;
import com.werkpilot.maintenance.application.MaintenanceTicketService;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController class MaintenanceTicketController {
  private final MaintenanceTicketService service; MaintenanceTicketController(MaintenanceTicketService service){this.service=service;}
  @GetMapping("/maintenance-tickets") List<TicketResponse> list(){return service.list().stream().map(this::response).toList();}
  @GetMapping("/maintenance-tickets/{id}") TicketDetailResponse get(@PathVariable UUID id){MaintenanceTicket t=service.get(id);return new TicketDetailResponse(response(t),service.comments(id));}
  @PostMapping("/maintenance-tickets") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) TicketResponse create(Authentication a,@Valid @RequestBody TicketRequest r){return response(service.create(principal(a),new MaintenanceTicketService.TicketCommand(r.title(),r.description(),r.priority(),r.factoryId(),r.lineId(),r.machineId(),r.anomalyId(),r.assigneeUserId(),r.dueDate())));}
  @PatchMapping("/maintenance-tickets/{id}/status") TicketResponse status(Authentication a,@PathVariable UUID id,@Valid @RequestBody TicketStatusRequest r){return response(service.changeStatus(principal(a),id,r.status(),r.note()));}
  @PostMapping("/maintenance-tickets/{id}/comments") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) MaintenanceTicketComment comment(Authentication a,@PathVariable UUID id,@Valid @RequestBody CommentRequest r){return service.addComment(principal(a),id,r.message());}
  private AuthenticatedPrincipal principal(Authentication a){return (AuthenticatedPrincipal)a.getPrincipal();} private TicketResponse response(MaintenanceTicket t){return new TicketResponse(t.id(),t.title(),t.description(),t.statusName(),t.priorityName(),t.factoryId(),t.lineId(),t.machineId(),t.anomalyId(),t.assigneeUserId(),t.dueDate(),t.resolutionNote(),t.cancellationReason(),t.createdByUserId(),t.createdAt(),t.updatedAt(),service.overdue(t));}
  record TicketRequest(@NotBlank String title,String description,String priority,UUID factoryId,UUID lineId,UUID machineId,UUID anomalyId,UUID assigneeUserId,LocalDate dueDate){} record TicketStatusRequest(@NotBlank String status,String note){} record CommentRequest(@NotBlank String message){} record TicketResponse(UUID id,String title,String description,String status,String priority,UUID factoryId,UUID lineId,UUID machineId,UUID anomalyId,UUID assigneeUserId,LocalDate dueDate,String resolutionNote,String cancellationReason,UUID createdByUserId,java.time.Instant createdAt,java.time.Instant updatedAt,boolean overdue){} record TicketDetailResponse(TicketResponse ticket,List<MaintenanceTicketComment> comments){}
}
