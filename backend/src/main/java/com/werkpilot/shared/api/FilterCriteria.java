package com.werkpilot.shared.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FilterCriteria(
        OffsetDateTime from,
        OffsetDateTime to,
        UUID factoryId,
        UUID lineId,
        UUID machineId,
        UUID productId,
        UUID shiftId,
        String anomalyType,
        String severity,
        String anomalyStatus,
        String ticketStatus,
        String priority,
        UUID assigneeId,
        UUID reasonId,
        UUID scrapCategoryId) {

    public FilterCriteria {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }
}
