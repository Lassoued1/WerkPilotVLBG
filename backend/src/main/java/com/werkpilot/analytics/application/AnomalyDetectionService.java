package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.BaselineQuality;
import com.werkpilot.analytics.domain.DetectionMethod;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectionService {

    public static final String DETECTOR_VERSION = "WP-S4-DETERMINISTIC-V1";
    private static final BigDecimal WARNING_Z_SCORE = new BigDecimal("3.000");
    private static final BigDecimal CRITICAL_Z_SCORE = new BigDecimal("4.000");
    private static final int MEDIUM_BASELINE_COUNT = 10;
    private static final int HIGH_BASELINE_COUNT = 30;

    public Optional<AnomalyDetectionCandidate> detect(AnomalyDetectionRequest request) {
        BigDecimal observed = request.observedValue();
        if (observed == null) {
            return Optional.empty();
        }

        BaselineStats stats = baselineStats(request);
        Optional<ThresholdHit> thresholdHit = thresholdHit(request, observed);
        Optional<ZScoreHit> zScoreHit = zScoreHit(observed, stats);
        if (thresholdHit.isEmpty() && zScoreHit.isEmpty()) {
            return Optional.empty();
        }

        ThresholdSeverity severity = strongest(thresholdHit.map(ThresholdHit::severity).orElse(null), zScoreHit.map(ZScoreHit::severity).orElse(null));
        DetectionMethod method = method(thresholdHit.isPresent(), zScoreHit.isPresent());
        String explanation = explanation(request, observed, stats, thresholdHit, zScoreHit);
        String identityKey = identityKey(request);
        String fingerprint = fingerprint(identityKey, severity.name(), method.name(), observed, stats.average(), stats.stddev(), stats.count(), explanation);

        return Optional.of(new AnomalyDetectionCandidate(
                identityKey,
                DETECTOR_VERSION,
                request.metricKey(),
                request.anomalyType(),
                severity,
                method,
                request.factoryId(),
                request.lineId(),
                request.machineId(),
                request.productId(),
                request.shiftId(),
                request.periodStart(),
                request.periodEnd(),
                observed.setScale(3, RoundingMode.HALF_UP),
                stats.average(),
                stats.stddev(),
                stats.count(),
                quality(stats.count()),
                zScoreHit.map(ZScoreHit::absoluteZScore).orElse(null),
                thresholdHit.map(ThresholdHit::thresholdRuleId).orElse(null),
                explanation,
                fingerprint));
    }

    private Optional<ThresholdHit> thresholdHit(AnomalyDetectionRequest request, BigDecimal observed) {
        return request.thresholdRules().stream()
                .filter(ThresholdRule::active)
                .filter(rule -> rule.metricKey() == request.metricKey())
                .filter(rule -> violates(rule, observed))
                .max(Comparator.comparing(ThresholdRule::severity))
                .map(rule -> new ThresholdHit(rule.id(), rule.severity()));
    }

    private Optional<ZScoreHit> zScoreHit(BigDecimal observed, BaselineStats stats) {
        if (stats.count() < MEDIUM_BASELINE_COUNT || stats.stddev().compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        BigDecimal zScore = observed.subtract(stats.average()).abs().divide(stats.stddev(), 3, RoundingMode.HALF_UP);
        if (zScore.compareTo(CRITICAL_Z_SCORE) >= 0) {
            return Optional.of(new ZScoreHit(ThresholdSeverity.CRITICAL, zScore));
        }
        if (zScore.compareTo(WARNING_Z_SCORE) >= 0) {
            return Optional.of(new ZScoreHit(ThresholdSeverity.WARNING, zScore));
        }
        return Optional.empty();
    }

    private BaselineStats baselineStats(AnomalyDetectionRequest request) {
        int count = request.baselineValues().size();
        if (count == 0) {
            return new BaselineStats(BigDecimal.ZERO.setScale(3), BigDecimal.ZERO.setScale(3), 0);
        }
        BigDecimal sum = request.baselineValues().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
        BigDecimal variance = request.baselineValues().stream()
                .map(value -> value.subtract(average))
                .map(delta -> delta.multiply(delta))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
        BigDecimal stddev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(6, RoundingMode.HALF_UP);
        return new BaselineStats(average.setScale(3, RoundingMode.HALF_UP), stddev.setScale(3, RoundingMode.HALF_UP), count);
    }

    private static boolean violates(ThresholdRule rule, BigDecimal observed) {
        return (rule.minValue() != null && observed.compareTo(rule.minValue()) < 0)
                || (rule.maxValue() != null && observed.compareTo(rule.maxValue()) > 0);
    }

    private static ThresholdSeverity strongest(ThresholdSeverity left, ThresholdSeverity right) {
        if (left == ThresholdSeverity.CRITICAL || right == ThresholdSeverity.CRITICAL) {
            return ThresholdSeverity.CRITICAL;
        }
        return ThresholdSeverity.WARNING;
    }

    private static DetectionMethod method(boolean threshold, boolean zScore) {
        if (threshold && zScore) {
            return DetectionMethod.Z_SCORE_AND_THRESHOLD;
        }
        return threshold ? DetectionMethod.THRESHOLD : DetectionMethod.Z_SCORE;
    }

    private static BaselineQuality quality(int count) {
        if (count >= HIGH_BASELINE_COUNT) {
            return BaselineQuality.HIGH;
        }
        return count >= MEDIUM_BASELINE_COUNT ? BaselineQuality.MEDIUM : BaselineQuality.LOW;
    }

    private static String explanation(
            AnomalyDetectionRequest request,
            BigDecimal observed,
            BaselineStats stats,
            Optional<ThresholdHit> thresholdHit,
            Optional<ZScoreHit> zScoreHit) {
        String source = thresholdHit.isPresent() && zScoreHit.isPresent()
                ? "Schwellwert und z-Score"
                : thresholdHit.isPresent() ? "Schwellwert" : "z-Score";
        String z = zScoreHit.map(hit -> "; zScore=" + hit.absoluteZScore()).orElse("");
        return "%s: %s beobachtet=%s, baseline=%s, samples=%d%s"
                .formatted(source, request.metricKey().name().toLowerCase(Locale.ROOT), observed, stats.average(), stats.count(), z);
    }

    private static String identityKey(AnomalyDetectionRequest request) {
        return "%s|%s|factory=%s|line=%s|machine=%s|product=%s|shift=%s|%s|%s"
                .formatted(
                        DETECTOR_VERSION,
                        request.anomalyType(),
                        value(request.factoryId()),
                        value(request.lineId()),
                        value(request.machineId()),
                        value(request.productId()),
                        value(request.shiftId()),
                        request.periodStart(),
                        request.periodEnd());
    }

    private static String value(UUID id) {
        return id == null ? "-" : id.toString();
    }

    private static String fingerprint(Object... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object value : values) {
                digest.update(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '|');
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JVM", exception);
        }
    }

    private record BaselineStats(BigDecimal average, BigDecimal stddev, int count) {
    }

    private record ThresholdHit(UUID thresholdRuleId, ThresholdSeverity severity) {
    }

    private record ZScoreHit(ThresholdSeverity severity, BigDecimal absoluteZScore) {
    }
}
