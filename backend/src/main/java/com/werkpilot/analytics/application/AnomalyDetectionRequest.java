package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.analytics.domain.ThresholdMetricKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnomalyDetectionRequest(
        ThresholdMetricKey metricKey,
        AnomalyType anomalyType,
        UUID factoryId,
        UUID lineId,
        UUID machineId,
        UUID productId,
        UUID shiftId,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal observedValue,
        List<BigDecimal> baselineValues,
        List<ThresholdRule> thresholdRules) {

    public AnomalyDetectionRequest {
        baselineValues = baselineValues == null ? List.of() : List.copyOf(baselineValues);
        thresholdRules = thresholdRules == null ? List.of() : List.copyOf(thresholdRules);
    }
}
