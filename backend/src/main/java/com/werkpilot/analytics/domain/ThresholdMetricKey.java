package com.werkpilot.analytics.domain;

public enum ThresholdMetricKey {
    ENERGY_KWH(true),
    ENERGY_PER_UNIT(true),
    OUTPUT_PER_HOUR(false),
    AVAILABILITY(false),
    DOWNTIME_MINUTES(false),
    SCRAP_RATE(false);

    private final boolean energyMetric;

    ThresholdMetricKey(boolean energyMetric) {
        this.energyMetric = energyMetric;
    }

    public boolean isEnergyMetric() {
        return energyMetric;
    }
}
