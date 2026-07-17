package com.werkpilot.analytics.application;

import java.time.Instant;
import java.util.UUID;

public record KpiQuery(
        Instant from,
        Instant to,
        UUID factoryId,
        UUID lineId,
        UUID machineId,
        UUID productId,
        UUID shiftId) {

    public KpiQuery {
        if (from == null) {
            throw new IllegalArgumentException("from is required");
        }
        if (to == null) {
            throw new IllegalArgumentException("to is required");
        }
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
    }
}
