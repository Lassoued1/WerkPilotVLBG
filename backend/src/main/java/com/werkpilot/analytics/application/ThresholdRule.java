package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdScopeType;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ThresholdRule(
        UUID id,
        ThresholdMetricKey metricKey,
        ThresholdScopeType scopeType,
        UUID scopeId,
        BigDecimal minValue,
        BigDecimal maxValue,
        ThresholdSeverity severity,
        boolean active,
        UUID createdByUserId,
        UUID updatedByUserId,
        Instant createdAt,
        Instant updatedAt) {

    public String metricKeyValue() {
        return metricKey.name();
    }

    public String scopeTypeValue() {
        return scopeType.name();
    }

    public String severityValue() {
        return severity.name();
    }
}
