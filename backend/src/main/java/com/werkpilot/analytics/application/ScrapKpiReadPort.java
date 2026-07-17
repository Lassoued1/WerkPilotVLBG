package com.werkpilot.analytics.application;

public interface ScrapKpiReadPort {

    ScrapTotals totals(KpiQuery query);
}
