package com.werkpilot.maintenance.application;

import com.werkpilot.maintenance.domain.TicketPriority;
import com.werkpilot.maintenance.domain.TicketStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceTicket(UUID id, String title, String description, TicketStatus status, TicketPriority priority, UUID factoryId, UUID lineId, UUID machineId, UUID anomalyId, UUID assigneeUserId, LocalDate dueDate, String resolutionNote, String cancellationReason, UUID createdByUserId, Instant createdAt, Instant updatedAt) {
  public String statusName() { return status.name(); }
  public String priorityName() { return priority.name(); }
}
