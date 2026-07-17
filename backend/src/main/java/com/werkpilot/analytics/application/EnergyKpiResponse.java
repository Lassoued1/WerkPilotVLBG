package com.werkpilot.analytics.application;

import java.math.BigDecimal;

public record EnergyKpiResponse(
        AppliedKpiFilters appliedFilters,
        BigDecimal totalEnergyKwh,
        KpiValue energyPerUnit) {
}
