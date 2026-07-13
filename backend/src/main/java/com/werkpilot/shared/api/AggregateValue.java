package com.werkpilot.shared.api;

import java.math.BigDecimal;

public record AggregateValue(
        BigDecimal value,
        String unit,
        boolean available,
        String reason) {

    public static AggregateValue available(BigDecimal value, String unit) {
        return new AggregateValue(value, unit, true, null);
    }

    public static AggregateValue unavailable(String unit, String reason) {
        return new AggregateValue(null, unit, false, reason);
    }

    public AggregateValue {
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("unit is required");
        }
        if (available && value == null) {
            throw new IllegalArgumentException("available aggregate values require a value");
        }
        if (!available && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("unavailable aggregate values require a reason");
        }
    }
}
