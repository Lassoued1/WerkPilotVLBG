package com.werkpilot.maintenance.application;

import java.time.Instant;
import java.util.UUID;
public record MaintenanceTicketComment(UUID id, UUID ticketId, UUID authorUserId, String message, Instant createdAt) { }
