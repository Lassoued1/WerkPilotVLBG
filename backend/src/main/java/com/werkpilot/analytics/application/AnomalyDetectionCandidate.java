package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.analytics.domain.BaselineQuality;
import com.werkpilot.analytics.domain.DetectionMethod;
import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnomalyDetectionCandidate(
        String identityKey,
        String detectorVersion,
        ThresholdMetricKey metricKey,
        AnomalyType anomalyType,
        ThresholdSeverity severity,
        DetectionMethod detectionMethod,
        UUID factoryId,
        UUID lineId,
        UUID machineId,
        UUID productId,
        UUID shiftId,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal observedValue,
        BigDecimal baselineAverage,
        BigDecimal baselineStddev,
        int baselineCount,
        BaselineQuality baselineQuality,
        BigDecimal zScore,
        UUID thresholdRuleId,
        String explanation,
        String fingerprint) {
}
