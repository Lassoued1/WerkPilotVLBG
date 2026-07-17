package com.werkpilot.analytics.application;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        AppliedKpiFilters appliedFilters,
        long totalUnitsProduced,
        BigDecimal totalEnergyKwh,
        long totalDowntimeMinutes,
        long totalScrapCount,
        KpiValue outputPerHour,
        KpiValue energyPerUnit,
        KpiValue availability,
        KpiValue scrapRate,
        List<ProductionTrendPoint> productionTrend,
        List<DowntimeParetoPoint> downtimePareto,
        List<EnergyTopConsumer> energyTopConsumers) {
}
