package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.werkpilot.analytics.application.AnomalyDetectionRequest;
import com.werkpilot.analytics.application.AnomalyDetectionService;
import com.werkpilot.analytics.application.ThresholdRule;
import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.analytics.domain.BaselineQuality;
import com.werkpilot.analytics.domain.DetectionMethod;
import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdScopeType;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnomalyDetectionServiceTest {

    private final AnomalyDetectionService service = new AnomalyDetectionService();

    @Test
    void baselineBelowTenUsesThresholdFallbackAndMarksLowQuality() {
        ThresholdRule threshold = new ThresholdRule(
                UUID.randomUUID(),
                ThresholdMetricKey.ENERGY_KWH,
                ThresholdScopeType.GLOBAL,
                null,
                null,
                bd("150"),
                ThresholdSeverity.CRITICAL,
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                Instant.now());

        var detected = service.detect(request(ThresholdMetricKey.ENERGY_KWH, AnomalyType.ENERGY_SPIKE, bd("175"), baseline(9), List.of(threshold)));

        assertThat(detected).isPresent();
        assertThat(detected.orElseThrow().detectionMethod()).isEqualTo(DetectionMethod.THRESHOLD);
        assertThat(detected.orElseThrow().severity()).isEqualTo(ThresholdSeverity.CRITICAL);
        assertThat(detected.orElseThrow().baselineQuality()).isEqualTo(BaselineQuality.LOW);
    }

    @Test
    void zScoreBoundariesAreDeterministicForNoWarningWarningAndCritical() {
        List<BigDecimal> baseline = List.of(bd("98"), bd("102"), bd("98"), bd("102"), bd("98"), bd("102"), bd("98"), bd("102"), bd("98"), bd("102"));

        assertThat(service.detect(request(ThresholdMetricKey.ENERGY_KWH, AnomalyType.ENERGY_SPIKE, bd("104"), baseline, List.of())))
                .isEmpty();

        var warning = service.detect(request(ThresholdMetricKey.ENERGY_KWH, AnomalyType.ENERGY_SPIKE, bd("106"), baseline, List.of()));
        assertThat(warning).isPresent();
        assertThat(warning.orElseThrow().severity()).isEqualTo(ThresholdSeverity.WARNING);
        assertThat(warning.orElseThrow().baselineQuality()).isEqualTo(BaselineQuality.MEDIUM);

        var critical = service.detect(request(ThresholdMetricKey.ENERGY_KWH, AnomalyType.ENERGY_SPIKE, bd("108"), baseline, List.of()));
        assertThat(critical).isPresent();
        assertThat(critical.orElseThrow().severity()).isEqualTo(ThresholdSeverity.CRITICAL);
        assertThat(critical.orElseThrow().zScore()).isEqualByComparingTo("4.000");
    }

    @Test
    void thirtySamplesProduceHighBaselineQuality() {
        var detected = service.detect(request(ThresholdMetricKey.DOWNTIME_MINUTES, AnomalyType.DOWNTIME_SPIKE, bd("108"), baseline(30), List.of()));

        assertThat(detected).isPresent();
        assertThat(detected.orElseThrow().baselineQuality()).isEqualTo(BaselineQuality.HIGH);
    }

    private static AnomalyDetectionRequest request(
            ThresholdMetricKey metricKey,
            AnomalyType type,
            BigDecimal observed,
            List<BigDecimal> baseline,
            List<ThresholdRule> thresholds) {
        return new AnomalyDetectionRequest(
                metricKey,
                type,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                Instant.parse("2026-07-15T08:00:00Z"),
                Instant.parse("2026-07-15T09:00:00Z"),
                observed,
                baseline,
                thresholds);
    }

    private static List<BigDecimal> baseline(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> index % 2 == 0 ? bd("98") : bd("102"))
                .toList();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
