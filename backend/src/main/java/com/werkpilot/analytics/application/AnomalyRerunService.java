package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.analytics.domain.ThresholdMetricKey;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnomalyRerunService {

    private static final int BASELINE_WINDOWS = 12;

    private final KpiQueryService kpiQueryService;
    private final ThresholdRulePort thresholdRulePort;
    private final AnomalyDetectionService anomalyDetectionService;
    private final AnomalyPort anomalyPort;
    private final RecommendationService recommendationService;
    private final RecommendationPort recommendationPort;

    public AnomalyRerunService(
            KpiQueryService kpiQueryService,
            ThresholdRulePort thresholdRulePort,
            AnomalyDetectionService anomalyDetectionService,
            AnomalyPort anomalyPort,
            RecommendationService recommendationService,
            RecommendationPort recommendationPort) {
        this.kpiQueryService = kpiQueryService;
        this.thresholdRulePort = thresholdRulePort;
        this.anomalyDetectionService = anomalyDetectionService;
        this.anomalyPort = anomalyPort;
        this.recommendationService = recommendationService;
        this.recommendationPort = recommendationPort;
    }

    @Transactional
    public AnomalyRerunResult rerun(KpiQuery query) {
        List<AnomalyDetectionCandidate> candidates = candidates(query);
        int created = 0;
        int superseded = 0;
        int unchanged = 0;
        Set<String> candidateIdentities = new HashSet<>();

        for (AnomalyDetectionCandidate candidate : candidates) {
            candidateIdentities.add(candidate.identityKey());
            var active = anomalyPort.findActiveByIdentityKey(candidate.identityKey());
            if (active.isEmpty()) {
                AnomalyRecord createdRecord = anomalyPort.create(candidate, null);
                recommendationPort.replaceForAnomaly(createdRecord.id(), recommendationService.recommendationsFor(createdRecord.id(), candidate));
                created++;
            } else if (active.get().fingerprint().equals(candidate.fingerprint())) {
                unchanged++;
            } else {
                AnomalyRecord successor = anomalyPort.create(candidate, active.get().id());
                recommendationPort.replaceForAnomaly(successor.id(), recommendationService.recommendationsFor(successor.id(), candidate));
                anomalyPort.supersede(active.get().id(), successor.id());
                superseded++;
                created++;
            }
        }

        for (AnomalyRecord active : anomalyPort.activeInWindow(query.from(), query.to())) {
            if (!candidateIdentities.contains(active.identityKey())) {
                anomalyPort.supersede(active.id(), null);
                superseded++;
            }
        }

        return new AnomalyRerunResult(created, superseded, unchanged, candidates.size());
    }

    private List<AnomalyDetectionCandidate> candidates(KpiQuery query) {
        List<AnomalyDetectionCandidate> candidates = new ArrayList<>();
        metricCandidates(query, ThresholdMetricKey.ENERGY_KWH, AnomalyType.ENERGY_SPIKE, energyTotal(query)).ifPresent(candidates::add);
        metricCandidates(query, ThresholdMetricKey.ENERGY_PER_UNIT, AnomalyType.ENERGY_PER_UNIT_SPIKE, kpiValue(kpiQueryService.energyKpis(query).energyPerUnit())).ifPresent(candidates::add);
        metricCandidates(query, ThresholdMetricKey.OUTPUT_PER_HOUR, AnomalyType.PRODUCTION_DROP, kpiValue(kpiQueryService.productionKpis(query).outputPerHour())).ifPresent(candidates::add);
        metricCandidates(query, ThresholdMetricKey.DOWNTIME_MINUTES, AnomalyType.DOWNTIME_SPIKE, BigDecimal.valueOf(kpiQueryService.downtimePareto(query).totalDowntimeMinutes())).ifPresent(candidates::add);
        metricCandidates(query, ThresholdMetricKey.SCRAP_RATE, AnomalyType.SCRAP_SPIKE, kpiValue(kpiQueryService.scrapRate(query).scrapRate())).ifPresent(candidates::add);
        return candidates;
    }

    private java.util.Optional<AnomalyDetectionCandidate> metricCandidates(
            KpiQuery query,
            ThresholdMetricKey metricKey,
            AnomalyType anomalyType,
            BigDecimal observed) {
        return anomalyDetectionService.detect(new AnomalyDetectionRequest(
                metricKey,
                anomalyType,
                query.factoryId(),
                query.lineId(),
                query.machineId(),
                query.productId(),
                query.shiftId(),
                query.from(),
                query.to(),
                observed,
                baselineValues(query, metricKey),
                thresholdRulePort.findActiveFor(metricKey, query.factoryId(), query.lineId(), query.machineId(), query.productId(), query.shiftId())));
    }

    private List<BigDecimal> baselineValues(KpiQuery query, ThresholdMetricKey metricKey) {
        Duration duration = Duration.between(query.from(), query.to());
        List<BigDecimal> values = new ArrayList<>();
        for (int index = BASELINE_WINDOWS; index >= 1; index--) {
            Instant from = query.from().minus(duration.multipliedBy(index));
            Instant to = from.plus(duration);
            KpiQuery baseline = new KpiQuery(from, to, query.factoryId(), query.lineId(), query.machineId(), query.productId(), query.shiftId());
            values.add(switch (metricKey) {
                case ENERGY_KWH -> energyTotal(baseline);
                case ENERGY_PER_UNIT -> kpiValue(kpiQueryService.energyKpis(baseline).energyPerUnit());
                case OUTPUT_PER_HOUR -> kpiValue(kpiQueryService.productionKpis(baseline).outputPerHour());
                case AVAILABILITY -> kpiValue(kpiQueryService.downtimePareto(baseline).availability());
                case DOWNTIME_MINUTES -> BigDecimal.valueOf(kpiQueryService.downtimePareto(baseline).totalDowntimeMinutes());
                case SCRAP_RATE -> kpiValue(kpiQueryService.scrapRate(baseline).scrapRate());
            });
        }
        return values;
    }

    private BigDecimal energyTotal(KpiQuery query) {
        return kpiQueryService.energyKpis(query).totalEnergyKwh();
    }

    private static BigDecimal kpiValue(KpiValue value) {
        return value.available() && value.value() != null ? value.value() : BigDecimal.ZERO;
    }

    public record AnomalyRerunResult(int created, int superseded, int unchanged, int detected) {
    }
}
