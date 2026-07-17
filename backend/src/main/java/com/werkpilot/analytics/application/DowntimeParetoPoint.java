package com.werkpilot.analytics.application;

import java.math.BigDecimal;
import java.util.UUID;

public record DowntimeParetoPoint(
        UUID reasonId,
        String reasonName,
        long downtimeMinutes,
        BigDecimal cumulativePercentage) {
}
