package com.werkpilot.analytics.application;

import java.math.BigDecimal;

public record KpiValue(
        BigDecimal value,
        String unit,
        boolean available,
        String reason) {

    public static KpiValue available(BigDecimal value, String unit) {
        return new KpiValue(value, unit, true, null);
    }

    public static KpiValue unavailable(String unit, String reason) {
        return new KpiValue(null, unit, false, reason);
    }
}
