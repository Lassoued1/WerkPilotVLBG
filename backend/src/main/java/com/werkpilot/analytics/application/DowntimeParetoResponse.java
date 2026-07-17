package com.werkpilot.analytics.application;

import java.util.List;

public record DowntimeParetoResponse(
        AppliedKpiFilters appliedFilters,
        long totalDowntimeMinutes,
        KpiValue availability,
        List<DowntimeParetoPoint> items) {
}
