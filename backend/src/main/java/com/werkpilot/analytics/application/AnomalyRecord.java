package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.AnomalyStatus;
import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.analytics.domain.BaselineQuality;
import com.werkpilot.analytics.domain.DetectionMethod;
import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AnomalyRecord(
        UUID id,
        String identityKey,
        String detectorVersion,
        ThresholdMetricKey metricKey,
        AnomalyType anomalyType,
        ThresholdSeverity severity,
        AnomalyStatus status,
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
        String fingerprint,
        UUID previousAnomalyId,
        UUID supersededByAnomalyId,
        Instant createdAt,
        Instant updatedAt) {

    public String metricKeyValue() {
        return metricKey.name();
    }

    public String anomalyTypeValue() {
        return anomalyType.name();
    }

    public String severityValue() {
        return severity.name();
    }

    public String statusValue() {
        return status.name();
    }

    public String detectionMethodValue() {
        return detectionMethod.name();
    }

    public String baselineQualityValue() {
        return baselineQuality.name();
    }
}
