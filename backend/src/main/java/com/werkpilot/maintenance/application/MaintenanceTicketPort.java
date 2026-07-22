package com.werkpilot.maintenance.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceTicketPort {

    Optional<MaintenanceTicket> findById(UUID id);

    Optional<MaintenanceTicket> findByAnomalyId(UUID anomalyId);

    List<MaintenanceTicket> list(TicketSearchCriteria criteria, UUID preferredAssigneeId);

    List<MaintenanceTicket> findByMachineAndIssueCategoryCreatedSince(UUID machineId, String issueCategory, Instant createdAfter);

    MaintenanceTicket create(MaintenanceTicket ticket);

    MaintenanceTicket update(MaintenanceTicket ticket);

    MaintenanceTicketComment addComment(MaintenanceTicketComment comment);

    List<MaintenanceTicketComment> comments(UUID ticketId);
}
