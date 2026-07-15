package com.werkpilot.energy.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EnergyMeasurementDraft(
        UUID id,
        UUID importJobId,
        Instant periodStart,
        Instant periodEnd,
        UUID factoryId,
        UUID lineId,
        UUID machineId,
        UUID shiftId,
        BigDecimal energyKwh) {
}
