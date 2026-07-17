package com.werkpilot.analytics.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductionKpiReadPort {

    ProductionTotals totals(KpiQuery query);

    Page<ProductionRecordView> listRecords(KpiQuery query, Pageable pageable);

    List<ProductionTrendPoint> hourlyTrend(KpiQuery query);

    List<ProductionRecordView> evidenceRows(KpiQuery query);
}
