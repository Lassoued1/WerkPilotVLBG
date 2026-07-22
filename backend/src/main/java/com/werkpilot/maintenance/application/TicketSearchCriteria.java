package com.werkpilot.maintenance.application;

import com.werkpilot.maintenance.domain.TicketPriority;
import com.werkpilot.maintenance.domain.TicketStatus;
import java.util.UUID;

public record TicketSearchCriteria(
        TicketStatus status,
        TicketPriority priority,
        UUID assigneeUserId,
        UUID machineId) {
}
