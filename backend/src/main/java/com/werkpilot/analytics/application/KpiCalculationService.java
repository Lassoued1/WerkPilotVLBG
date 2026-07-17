package com.werkpilot.analytics.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class KpiCalculationService {

    private static final int SCALE = 3;

    public KpiValue outputPerHour(long unitsProduced, BigDecimal productionHours) {
        if (productionHours == null || productionHours.compareTo(BigDecimal.ZERO) <= 0) {
            return KpiValue.unavailable("units_per_hour", "NO_PRODUCTION_TIME");
        }
        return KpiValue.available(
                BigDecimal.valueOf(unitsProduced).divide(productionHours, SCALE, RoundingMode.HALF_UP),
                "units_per_hour");
    }

    public KpiValue energyPerUnit(BigDecimal energyKwh, long unitsProduced) {
        if (unitsProduced <= 0) {
            return KpiValue.unavailable("kWh_per_unit", "NO_UNITS_PRODUCED");
        }
        return KpiValue.available(
                nullToZero(energyKwh).divide(BigDecimal.valueOf(unitsProduced), SCALE, RoundingMode.HALF_UP),
                "kWh_per_unit");
    }

    public KpiValue scrapRate(long scrapCount, long unitsProduced) {
        if (unitsProduced <= 0) {
            return KpiValue.unavailable("percent", "NO_UNITS_PRODUCED");
        }
        return KpiValue.available(
                BigDecimal.valueOf(scrapCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(unitsProduced), SCALE, RoundingMode.HALF_UP),
                "percent");
    }

    public KpiValue availability(long plannedMinutes, long downtimeMinutes) {
        if (plannedMinutes <= 0) {
            return KpiValue.unavailable("percent", "NO_PLANNED_MINUTES");
        }
        BigDecimal availableMinutes = BigDecimal.valueOf(Math.max(0, plannedMinutes - downtimeMinutes));
        return KpiValue.available(
                availableMinutes
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(plannedMinutes), SCALE, RoundingMode.HALF_UP),
                "percent");
    }

    public long backlogUnits(long requiredUnits, long producedUnits) {
        return Math.max(0, requiredUnits - producedUnits);
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
