package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.AnomalyType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    public static final String DISCLAIMER_DE = "Hinweis: Diese Empfehlung ist eine deterministische Entscheidungshilfe und ersetzt keine fachliche Pruefung durch das Werkteam.";
    public static final String TEMPLATE_VERSION = "2026-07-s4-v1";

    public List<RecommendationRecord> recommendationsFor(AnomalyRecord anomaly) {
        return List.of(new RecommendationRecord(
                UUID.randomUUID(),
                anomaly.id(),
                templateCode(anomaly.anomalyType()),
                TEMPLATE_VERSION,
                message(anomaly),
                DISCLAIMER_DE,
                Instant.now()));
    }

    public List<RecommendationRecord> recommendationsFor(UUID anomalyId, AnomalyDetectionCandidate candidate) {
        AnomalyRecord synthetic = new AnomalyRecord(
                anomalyId,
                candidate.identityKey(),
                candidate.detectorVersion(),
                candidate.metricKey(),
                candidate.anomalyType(),
                candidate.severity(),
                null,
                candidate.detectionMethod(),
                candidate.factoryId(),
                candidate.lineId(),
                candidate.machineId(),
                candidate.productId(),
                candidate.shiftId(),
                candidate.periodStart(),
                candidate.periodEnd(),
                candidate.observedValue(),
                candidate.baselineAverage(),
                candidate.baselineStddev(),
                candidate.baselineCount(),
                candidate.baselineQuality(),
                candidate.zScore(),
                candidate.thresholdRuleId(),
                candidate.explanation(),
                candidate.fingerprint(),
                null,
                null,
                Instant.now(),
                Instant.now());
        return recommendationsFor(synthetic);
    }

    private static String templateCode(AnomalyType type) {
        return switch (type) {
            case ENERGY_SPIKE, ENERGY_PER_UNIT_SPIKE -> "ENERGY_CHECK";
            case PRODUCTION_DROP -> "PRODUCTION_OUTPUT_CHECK";
            case DOWNTIME_SPIKE -> "DOWNTIME_ROOT_CAUSE_CHECK";
            case SCRAP_SPIKE -> "QUALITY_SCRAP_CHECK";
            case RECURRING_TICKET_PATTERN -> "RECURRING_TICKET_ROOT_CAUSE";
        };
    }

    private static String message(AnomalyRecord anomaly) {
        return switch (anomaly.anomalyType()) {
            case ENERGY_SPIKE, ENERGY_PER_UNIT_SPIKE ->
                    "Energieverbrauch pruefen: Zaehlerstand, Maschinenzustand und Schichtkontext vergleichen.";
            case PRODUCTION_DROP ->
                    "Produktionsleistung pruefen: Auftrag, Materialfluss und Maschinenstillstaende gegen den Zeitraum abgleichen.";
            case DOWNTIME_SPIKE ->
                    "Stillstandsursache pruefen: Pareto-Grund, Wartungsbedarf und Wiederholungen an der Maschine bewerten.";
            case SCRAP_SPIKE ->
                    "Ausschuss pruefen: Kategorie, Produktcharge und Prozessparameter im betroffenen Zeitraum nachvollziehen.";
            case RECURRING_TICKET_PATTERN ->
                    "Grundursachenanalyse einleiten: Gleichartige Wartungsfaelle an derselben Maschine sind innerhalb von 30 Tagen wiederholt aufgetreten.";
        };
    }
}
