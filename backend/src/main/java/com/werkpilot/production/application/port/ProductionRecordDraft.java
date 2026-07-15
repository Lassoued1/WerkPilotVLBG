package com.werkpilot.production.application.port;

import java.time.Instant;
import java.util.UUID;

public record ProductionRecordDraft(
        UUID id,
        UUID importJobId,
        Instant periodStart,
        Instant periodEnd,
        UUID factoryId,
        UUID lineId,
        UUID machineId,
        UUID productId,
        UUID shiftId,
        int unitsProduced,
        String batchCode) {
}