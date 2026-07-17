package com.werkpilot.analytics.api;

import com.werkpilot.analytics.application.DashboardSummaryResponse;
import com.werkpilot.analytics.application.DowntimeParetoResponse;
import com.werkpilot.analytics.application.EnergyKpiResponse;
import com.werkpilot.analytics.application.EnergyTopConsumer;
import com.werkpilot.analytics.application.KpiQuery;
import com.werkpilot.analytics.application.KpiQueryService;
import com.werkpilot.analytics.application.ProductionRecordPage;
import com.werkpilot.analytics.application.ProductionKpiResponse;
import com.werkpilot.analytics.application.ProductionRecordView;
import com.werkpilot.analytics.application.ProductionTrendPoint;
import com.werkpilot.analytics.application.ScrapRateResponse;
import com.werkpilot.shared.api.PageResponse;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class KpiAnalyticsController {

    private final KpiQueryService kpiQueryService;

    KpiAnalyticsController(KpiQueryService kpiQueryService) {
        this.kpiQueryService = kpiQueryService;
    }

    @GetMapping("/production/kpis")
    ProductionKpiResponse productionKpis(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId) {
        return kpiQueryService.productionKpis(query(from, to, factoryId, lineId, machineId, productId, shiftId));
    }

    @GetMapping("/production/records")
    PageResponse<ProductionRecordView> productionRecords(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Invalid pagination parameters.");
        }
        ProductionRecordPage recordPage = kpiQueryService.productionRecords(
                query(from, to, factoryId, lineId, machineId, productId, shiftId),
                page,
                size);
        return new PageResponse<>(
                recordPage.items(),
                recordPage.page(),
                recordPage.size(),
                recordPage.totalElements(),
                recordPage.totalPages());
    }

    @GetMapping("/production/trends")
    List<ProductionTrendPoint> productionTrends(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId) {
        return kpiQueryService.productionTrend(query(from, to, factoryId, lineId, machineId, productId, shiftId));
    }

    @GetMapping(path = "/production/evidence.csv", produces = "text/csv")
    String productionEvidenceCsv(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId) {
        return kpiQueryService.productionEvidenceCsv(query(from, to, factoryId, lineId, machineId, productId, shiftId));
    }

    @GetMapping("/energy/kpis")
    EnergyKpiResponse energyKpis(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId) {
        return kpiQueryService.energyKpis(query(from, to, factoryId, lineId, machineId, productId, shiftId));
    }

    @GetMapping("/energy/top-consumers")
    List<EnergyTopConsumer> energyTopConsumers(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId) {
        return kpiQueryService.energyTopConsumers(query(from, to, factoryId, lineId, machineId, productId, shiftId));
    }

    @GetMapping("/downtime/pareto")
    DowntimeParetoResponse downtimePareto(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId) {
        return kpiQueryService.downtimePareto(query(from, to, factoryId, lineId, machineId, productId, shiftId));
    }

    @GetMapping("/quality/scrap-rate")
    ScrapRateResponse scrapRate(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId) {
        return kpiQueryService.scrapRate(query(from, to, factoryId, lineId, machineId, productId, shiftId));
    }

    @GetMapping("/dashboard/summary")
    DashboardSummaryResponse dashboardSummary(
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId) {
        return kpiQueryService.dashboardSummary(query(from, to, factoryId, lineId, machineId, productId, shiftId));
    }

    private static KpiQuery query(
            OffsetDateTime from,
            OffsetDateTime to,
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID productId,
            UUID shiftId) {
        try {
            return new KpiQuery(
                    from == null ? null : from.toInstant(),
                    to == null ? null : to.toInstant(),
                    factoryId,
                    lineId,
                    machineId,
                    productId,
                    shiftId);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, exception.getMessage());
        }
    }
}
