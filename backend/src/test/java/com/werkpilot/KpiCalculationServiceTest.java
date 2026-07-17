package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.werkpilot.analytics.application.KpiCalculationService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KpiCalculationServiceTest {

    private final KpiCalculationService service = new KpiCalculationService();

    @Test
    void calculatesCoreKpisWithStablePrecision() {
        assertThat(service.outputPerHour(120, new BigDecimal("2.5")).value()).isEqualByComparingTo("48.000");
        assertThat(service.energyPerUnit(new BigDecimal("60.000"), 120).value()).isEqualByComparingTo("0.500");
        assertThat(service.scrapRate(3, 120).value()).isEqualByComparingTo("2.500");
        assertThat(service.availability(480, 30).value()).isEqualByComparingTo("93.750");
        assertThat(service.backlogUnits(500, 380)).isEqualTo(120);
        assertThat(service.backlogUnits(500, 520)).isZero();
    }

    @Test
    void returnsUnavailableAggregatesForMissingDenominators() {
        assertThat(service.outputPerHour(120, BigDecimal.ZERO).available()).isFalse();
        assertThat(service.outputPerHour(120, BigDecimal.ZERO).reason()).isEqualTo("NO_PRODUCTION_TIME");
        assertThat(service.energyPerUnit(new BigDecimal("60.000"), 0).available()).isFalse();
        assertThat(service.energyPerUnit(new BigDecimal("60.000"), 0).reason()).isEqualTo("NO_UNITS_PRODUCED");
        assertThat(service.scrapRate(3, 0).available()).isFalse();
        assertThat(service.availability(0, 30).available()).isFalse();
    }
}
