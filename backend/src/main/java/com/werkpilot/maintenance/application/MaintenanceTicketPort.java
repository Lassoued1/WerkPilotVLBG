package com.werkpilot.maintenance.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface MaintenanceTicketPort { Optional<MaintenanceTicket> findById(UUID id); List<MaintenanceTicket> list(); MaintenanceTicket create(MaintenanceTicket ticket); MaintenanceTicket update(MaintenanceTicket ticket); MaintenanceTicketComment addComment(MaintenanceTicketComment comment); List<MaintenanceTicketComment> comments(UUID ticketId); }
