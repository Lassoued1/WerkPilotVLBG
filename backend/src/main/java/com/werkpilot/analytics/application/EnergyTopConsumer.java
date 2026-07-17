package com.werkpilot.analytics.application;

import java.math.BigDecimal;
import java.util.UUID;

public record EnergyTopConsumer(
        UUID lineId,
        UUID machineId,
        BigDecimal energyKwh) {
}
