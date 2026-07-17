package com.werkpilot.analytics.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KpiQueryService {

    private final ProductionKpiReadPort productionKpiReadPort;
    private final EnergyKpiReadPort energyKpiReadPort;
    private final DowntimeKpiReadPort downtimeKpiReadPort;
    private final ScrapKpiReadPort scrapKpiReadPort;
    private final KpiCalculationService kpiCalculationService;

    public KpiQueryService(
            ProductionKpiReadPort productionKpiReadPort,
            EnergyKpiReadPort energyKpiReadPort,
            DowntimeKpiReadPort downtimeKpiReadPort,
            ScrapKpiReadPort scrapKpiReadPort,
            KpiCalculationService kpiCalculationService) {
        this.productionKpiReadPort = productionKpiReadPort;
        this.energyKpiReadPort = energyKpiReadPort;
        this.downtimeKpiReadPort = downtimeKpiReadPort;
        this.scrapKpiReadPort = scrapKpiReadPort;
        this.kpiCalculationService = kpiCalculationService;
    }

    public ProductionKpiResponse productionKpis(KpiQuery query) {
        ProductionTotals totals = productionKpiReadPort.totals(query);
        return new ProductionKpiResponse(
                AppliedKpiFilters.from(query),
                totals.unitsProduced(),
                kpiCalculationService.outputPerHour(totals.unitsProduced(), totals.productionHours()));
    }

    public EnergyKpiResponse energyKpis(KpiQuery query) {
        ProductionTotals productionTotals = productionKpiReadPort.totals(query);
        EnergyTotals energyTotals = energyKpiReadPort.totals(query);
        return new EnergyKpiResponse(
                AppliedKpiFilters.from(query),
                energyTotals.energyKwh(),
                kpiCalculationService.energyPerUnit(energyTotals.energyKwh(), productionTotals.unitsProduced()));
    }

    public ProductionRecordPage productionRecords(KpiQuery query, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<ProductionRecordView> page = productionKpiReadPort.listRecords(query, pageable);
        return new ProductionRecordPage(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    public List<ProductionTrendPoint> productionTrend(KpiQuery query) {
        return productionKpiReadPort.hourlyTrend(query);
    }

    public String productionEvidenceCsv(KpiQuery query) {
        StringBuilder csv = new StringBuilder("period_start,period_end,factory_id,line_id,machine_id,product_id,shift_id,units_produced,batch_code,import_job_id\n");
        for (ProductionRecordView row : productionKpiReadPort.evidenceRows(query)) {
            csv.append(row.periodStart()).append(',')
                    .append(row.periodEnd()).append(',')
                    .append(row.factoryId()).append(',')
                    .append(row.lineId()).append(',')
                    .append(nullToEmpty(row.machineId())).append(',')
                    .append(nullToEmpty(row.productId())).append(',')
                    .append(row.shiftId()).append(',')
                    .append(row.unitsProduced()).append(',')
                    .append(csvCell(row.batchCode())).append(',')
                    .append(row.importJobId()).append('\n');
        }
        return csv.toString();
    }

    public List<EnergyTopConsumer> energyTopConsumers(KpiQuery query) {
        return energyKpiReadPort.topConsumers(query, 10);
    }

    public DowntimeParetoResponse downtimePareto(KpiQuery query) {
        DowntimeTotals totals = downtimeKpiReadPort.totals(query);
        return new DowntimeParetoResponse(
                AppliedKpiFilters.from(query),
                totals.downtimeMinutes(),
                kpiCalculationService.availability(totals.plannedMinutes(), totals.downtimeMinutes()),
                downtimeKpiReadPort.pareto(query));
    }

    public ScrapRateResponse scrapRate(KpiQuery query) {
        ProductionTotals productionTotals = productionKpiReadPort.totals(query);
        ScrapTotals scrapTotals = scrapKpiReadPort.totals(query);
        return new ScrapRateResponse(
                AppliedKpiFilters.from(query),
                scrapTotals.scrapCount(),
                productionTotals.unitsProduced(),
                kpiCalculationService.scrapRate(scrapTotals.scrapCount(), productionTotals.unitsProduced()));
    }

    public DashboardSummaryResponse dashboardSummary(KpiQuery query) {
        ProductionTotals productionTotals = productionKpiReadPort.totals(query);
        EnergyTotals energyTotals = energyKpiReadPort.totals(query);
        DowntimeTotals downtimeTotals = downtimeKpiReadPort.totals(query);
        ScrapTotals scrapTotals = scrapKpiReadPort.totals(query);
        return new DashboardSummaryResponse(
                AppliedKpiFilters.from(query),
                productionTotals.unitsProduced(),
                energyTotals.energyKwh(),
                downtimeTotals.downtimeMinutes(),
                scrapTotals.scrapCount(),
                kpiCalculationService.outputPerHour(productionTotals.unitsProduced(), productionTotals.productionHours()),
                kpiCalculationService.energyPerUnit(energyTotals.energyKwh(), productionTotals.unitsProduced()),
                kpiCalculationService.availability(downtimeTotals.plannedMinutes(), downtimeTotals.downtimeMinutes()),
                kpiCalculationService.scrapRate(scrapTotals.scrapCount(), productionTotals.unitsProduced()),
                productionKpiReadPort.hourlyTrend(query),
                downtimeKpiReadPort.pareto(query),
                energyKpiReadPort.topConsumers(query, 10));
    }

    private static String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String csvCell(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.startsWith("=") || escaped.startsWith("+") || escaped.startsWith("-") || escaped.startsWith("@")) {
            escaped = "'" + escaped;
        }
        return "\"" + escaped + "\"";
    }
}
