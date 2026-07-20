package com.werkpilot.analytics.api;

import com.werkpilot.analytics.application.AnomalyQueryService;
import com.werkpilot.analytics.application.AnomalyQueryService.AnomalyDetail;
import com.werkpilot.analytics.application.AnomalyQueryService.AnomalyPage;
import com.werkpilot.analytics.application.AnomalyRecord;
import com.werkpilot.analytics.application.AnomalyRerunService;
import com.werkpilot.analytics.application.AnomalyRerunService.AnomalyRerunResult;
import com.werkpilot.analytics.application.KpiQuery;
import com.werkpilot.analytics.application.RecommendationRecord;
import com.werkpilot.shared.api.PageResponse;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AnomalyController {

    private final AnomalyQueryService anomalyQueryService;
    private final AnomalyRerunService anomalyRerunService;

    AnomalyController(AnomalyQueryService anomalyQueryService, AnomalyRerunService anomalyRerunService) {
        this.anomalyQueryService = anomalyQueryService;
        this.anomalyRerunService = anomalyRerunService;
    }

    @GetMapping("/anomalies")
    PageResponse<AnomalyResponse> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID factoryId,
            @RequestParam(required = false) UUID lineId,
            @RequestParam(required = false) UUID machineId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) String anomalyType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String anomalyStatus,
            @RequestParam(defaultValue = "false") boolean includeSuperseded,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AnomalyPage anomalies = anomalyQueryService.list(
                from,
                to,
                factoryId,
                lineId,
                machineId,
                productId,
                shiftId,
                anomalyType,
                severity,
                anomalyStatus,
                includeSuperseded,
                page,
                size);
        return new PageResponse<>(
                anomalies.items().stream().map(AnomalyController::response).toList(),
                anomalies.page(),
                anomalies.size(),
                anomalies.totalElements(),
                anomalies.totalPages());
    }

    @GetMapping("/anomalies/{id}")
    AnomalyDetailResponse get(@PathVariable UUID id) {
        AnomalyDetail detail = anomalyQueryService.get(id);
        return new AnomalyDetailResponse(
                response(detail.anomaly()),
                detail.recommendations().stream().map(AnomalyController::recommendation).toList());
    }

    @PatchMapping("/anomalies/{id}/status")
    AnomalyResponse updateStatus(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody AnomalyStatusRequest request) {
        return response(anomalyQueryService.updateStatus(id, request.status(), principal(authentication)));
    }

    @PostMapping("/anomalies/rerun")
    AnomalyRerunResult rerun(@Valid @RequestBody AnomalyRerunRequest request) {
        return anomalyRerunService.rerun(new KpiQuery(
                request.from(),
                request.to(),
                request.factoryId(),
                request.lineId(),
                request.machineId(),
                request.productId(),
                request.shiftId()));
    }

    private static AuthenticatedPrincipal principal(Authentication authentication) {
        return (AuthenticatedPrincipal) authentication.getPrincipal();
    }

    private static AnomalyResponse response(AnomalyRecord anomaly) {
        return new AnomalyResponse(
                anomaly.id(),
                anomaly.metricKeyValue(),
                anomaly.anomalyTypeValue(),
                anomaly.severityValue(),
                anomaly.statusValue(),
                anomaly.detectionMethodValue(),
                anomaly.factoryId(),
                anomaly.lineId(),
                anomaly.machineId(),
                anomaly.productId(),
                anomaly.shiftId(),
                anomaly.periodStart(),
                anomaly.periodEnd(),
                anomaly.observedValue(),
                anomaly.baselineAverage(),
                anomaly.baselineStddev(),
                anomaly.baselineCount(),
                anomaly.baselineQualityValue(),
                anomaly.zScore(),
                anomaly.thresholdRuleId(),
                anomaly.explanation(),
                anomaly.previousAnomalyId(),
                anomaly.supersededByAnomalyId(),
                anomaly.createdAt(),
                anomaly.updatedAt());
    }

    private static RecommendationResponse recommendation(RecommendationRecord recommendation) {
        return new RecommendationResponse(
                recommendation.id(),
                recommendation.templateCode(),
                recommendation.templateVersion(),
                recommendation.messageDe(),
                recommendation.disclaimerDe());
    }

    public record AnomalyRerunRequest(
            Instant from,
            Instant to,
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID productId,
            UUID shiftId) {
    }

    public record AnomalyStatusRequest(@Size(max = 20) String status) {
    }

    public record AnomalyDetailResponse(AnomalyResponse anomaly, List<RecommendationResponse> recommendations) {
    }

    public record RecommendationResponse(
            UUID id,
            String templateCode,
            String templateVersion,
            String messageDe,
            String disclaimerDe) {
    }

    public record AnomalyResponse(
            UUID id,
            String metricKey,
            String anomalyType,
            String severity,
            String status,
            String detectionMethod,
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID productId,
            UUID shiftId,
            Instant periodStart,
            Instant periodEnd,
            BigDecimal observedValue,
            BigDecimal baselineAverage,
            BigDecimal baselineStddev,
            int baselineCount,
            String baselineQuality,
            BigDecimal zScore,
            UUID thresholdRuleId,
            String explanation,
            UUID previousAnomalyId,
            UUID supersededByAnomalyId,
            Instant createdAt,
            Instant updatedAt) {
    }
}
