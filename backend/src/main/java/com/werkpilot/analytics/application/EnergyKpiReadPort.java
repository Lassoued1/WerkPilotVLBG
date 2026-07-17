package com.werkpilot.analytics.application;

import java.util.List;

public interface EnergyKpiReadPort {

    EnergyTotals totals(KpiQuery query);

    List<EnergyTopConsumer> topConsumers(KpiQuery query, int limit);
}
