package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.AnomalyStatus;
import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnomalyQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AnomalyPort anomalyPort;
    private final RecommendationPort recommendationPort;
    private final AuditEventPort auditEventPort;

    public AnomalyQueryService(AnomalyPort anomalyPort, RecommendationPort recommendationPort, AuditEventPort auditEventPort) {
        this.anomalyPort = anomalyPort;
        this.recommendationPort = recommendationPort;
        this.auditEventPort = auditEventPort;
    }

    @Transactional(readOnly = true)
    public AnomalyPage list(
            Instant from,
            Instant to,
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID productId,
            UUID shiftId,
            String anomalyType,
            String severity,
            String anomalyStatus,
            boolean includeSuperseded,
            int page,
            int size) {
        return list(new AnomalySearchCriteria(
                from,
                to,
                factoryId,
                lineId,
                machineId,
                productId,
                shiftId,
                parseOptionalEnum(anomalyType, AnomalyType.class, "anomalyType"),
                parseOptionalEnum(severity, ThresholdSeverity.class, "severity"),
                parseOptionalEnum(anomalyStatus, AnomalyStatus.class, "anomalyStatus"),
                includeSuperseded), page, size);
    }

    @Transactional(readOnly = true)
    public AnomalyPage list(AnomalySearchCriteria criteria, int page, int size) {
        if (page < 0 || size < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Invalid pagination parameters.");
        }
        Page<AnomalyRecord> anomalies = anomalyPort.search(
                criteria,
                PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("periodStart").descending()));
        return new AnomalyPage(anomalies.getContent(), anomalies.getNumber(), anomalies.getSize(), anomalies.getTotalElements(), anomalies.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AnomalyDetail get(UUID id) {
        AnomalyRecord anomaly = anomalyPort.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Anomaly was not found."));
        return new AnomalyDetail(anomaly, recommendationPort.findByAnomalyId(id));
    }

    @Transactional
    public AnomalyRecord updateStatus(UUID id, String requestedStatus, AuthenticatedPrincipal actor) {
        AnomalyStatus status = parseStatus(requestedStatus);
        if (status == AnomalyStatus.SUPERSEDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "SUPERSEDED is system-assigned only.");
        }
        if (!actor.roles().contains("ADMIN")
                && !actor.roles().contains("PRODUCTION_MANAGER")
                && !actor.roles().contains("ENERGY_MANAGER")) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Only managers and ADMIN can change anomaly status.");
        }
        AnomalyRecord existing = get(id).anomaly();
        if (existing.status() == AnomalyStatus.SUPERSEDED) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE_VIOLATION, "Superseded anomalies are terminal.");
        }
        AnomalyRecord updated = anomalyPort.updateStatus(id, status);
        auditEventPort.append(
                AuditEventType.ANOMALY_STATUS_CHANGED,
                actor.userId(),
                null,
                "anomalyId=%s; oldStatus=%s; newStatus=%s".formatted(id, existing.status(), status));
        return updated;
    }

    private static AnomalyStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "status is required.");
        }
        try {
            return AnomalyStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Unsupported anomaly status.");
        }
    }

    private static <T extends Enum<T>> T parseOptionalEnum(String value, Class<T> enumType, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Unsupported " + field + ".");
        }
    }

    public record AnomalyPage(List<AnomalyRecord> items, int page, int size, long totalElements, int totalPages) {
        public AnomalyPage {
            items = List.copyOf(items);
        }
    }

    public record AnomalyDetail(AnomalyRecord anomaly, List<RecommendationRecord> recommendations) {
        public AnomalyDetail {
            recommendations = List.copyOf(recommendations);
        }
    }
}
