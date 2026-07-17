package com.werkpilot.analytics.application;

public record ProductionKpiResponse(
        AppliedKpiFilters appliedFilters,
        long totalUnitsProduced,
        KpiValue outputPerHour) {
}
