package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.werkpilot.analytics.application.AnomalyRecord;
import com.werkpilot.analytics.application.RecommendationService;
import com.werkpilot.analytics.domain.AnomalyStatus;
import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.analytics.domain.BaselineQuality;
import com.werkpilot.analytics.domain.DetectionMethod;
import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {

    private final RecommendationService service = new RecommendationService();

    @Test
    void recommendationCarriesVersionedTemplateAndExactGermanDisclaimer() {
        var recommendations = service.recommendationsFor(new AnomalyRecord(
                UUID.randomUUID(),
                "identity",
                "detector",
                ThresholdMetricKey.SCRAP_RATE,
                AnomalyType.SCRAP_SPIKE,
                ThresholdSeverity.WARNING,
                AnomalyStatus.NEW,
                DetectionMethod.Z_SCORE,
                null,
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-07-15T08:00:00Z"),
                Instant.parse("2026-07-15T09:00:00Z"),
                new BigDecimal("8.000"),
                new BigDecimal("2.000"),
                new BigDecimal("1.000"),
                12,
                BaselineQuality.MEDIUM,
                new BigDecimal("6.000"),
                null,
                "test",
                "fingerprint",
                null,
                null,
                Instant.now(),
                Instant.now()));

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.getFirst().templateCode()).isEqualTo("QUALITY_SCRAP_CHECK");
        assertThat(recommendations.getFirst().templateVersion()).isEqualTo(RecommendationService.TEMPLATE_VERSION);
        assertThat(recommendations.getFirst().disclaimerDe()).isEqualTo(RecommendationService.DISCLAIMER_DE);
    }
}
