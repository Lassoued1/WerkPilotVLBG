package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdScopeType;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.math.BigDecimal;
import java.util.UUID;

public record ThresholdRuleDraft(
        ThresholdMetricKey metricKey,
        ThresholdScopeType scopeType,
        UUID scopeId,
        BigDecimal minValue,
        BigDecimal maxValue,
        ThresholdSeverity severity,
        boolean active,
        UUID actorUserId) {
}
