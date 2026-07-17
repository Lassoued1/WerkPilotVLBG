package com.werkpilot.analytics.application;

import java.time.Instant;
import java.util.UUID;

public record ProductionRecordView(
        UUID id,
        Instant periodStart,
        Instant periodEnd,
        UUID factoryId,
        UUID lineId,
        UUID machineId,
        UUID productId,
        UUID shiftId,
        int unitsProduced,
        String batchCode,
        UUID importJobId) {
}
