package com.werkpilot.analytics.application;

import java.time.Instant;
import java.util.UUID;

public record AppliedKpiFilters(
        Instant from,
        Instant to,
        UUID factoryId,
        UUID lineId,
        UUID machineId,
        UUID productId,
        UUID shiftId) {

    static AppliedKpiFilters from(KpiQuery query) {
        return new AppliedKpiFilters(
                query.from(),
                query.to(),
                query.factoryId(),
                query.lineId(),
                query.machineId(),
                query.productId(),
                query.shiftId());
    }
}
