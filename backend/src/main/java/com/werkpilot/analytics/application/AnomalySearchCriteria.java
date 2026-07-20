package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.AnomalyStatus;
import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.time.Instant;
import java.util.UUID;

public record AnomalySearchCriteria(
        Instant from,
        Instant to,
        UUID factoryId,
        UUID lineId,
        UUID machineId,
        UUID productId,
        UUID shiftId,
        AnomalyType anomalyType,
        ThresholdSeverity severity,
        AnomalyStatus anomalyStatus,
        boolean includeSuperseded) {
}
