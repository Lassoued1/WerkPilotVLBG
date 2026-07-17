package com.werkpilot.analytics.application;

import java.util.List;

public interface DowntimeKpiReadPort {

    DowntimeTotals totals(KpiQuery query);

    List<DowntimeParetoPoint> pareto(KpiQuery query);
}
