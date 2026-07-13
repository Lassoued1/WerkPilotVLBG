package com.werkpilot.masterdata.application.port;

import java.time.Instant;
import java.util.UUID;

public record SystemSettings(
        boolean energyThresholdDelegationEnabled,
        UUID updatedByUserId,
        Instant createdAt,
        Instant updatedAt) {
}
