package com.werkpilot.analytics.application;

public record ScrapRateResponse(
        AppliedKpiFilters appliedFilters,
        long totalScrapCount,
        long totalUnitsProduced,
        KpiValue scrapRate) {
}
