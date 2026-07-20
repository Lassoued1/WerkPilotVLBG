package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdScopeType;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.masterdata.application.MasterDataKind;
import com.werkpilot.masterdata.application.port.MasterDataPort;
import com.werkpilot.masterdata.application.port.SystemSettingsPort;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import java.math.BigDecimal;
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
public class ThresholdAdministrationService {

    private static final String ADMIN = "ADMIN";
    private static final int MAX_PAGE_SIZE = 100;

    private final ThresholdRulePort thresholdRulePort;
    private final MasterDataPort masterDataPort;
    private final SystemSettingsPort systemSettingsPort;
    private final AuditEventPort auditEventPort;

    public ThresholdAdministrationService(
            ThresholdRulePort thresholdRulePort,
            MasterDataPort masterDataPort,
            SystemSettingsPort systemSettingsPort,
            AuditEventPort auditEventPort) {
        this.thresholdRulePort = thresholdRulePort;
        this.masterDataPort = masterDataPort;
        this.systemSettingsPort = systemSettingsPort;
        this.auditEventPort = auditEventPort;
    }

    @Transactional(readOnly = true)
    public ThresholdRulePage list(String metricKey, String scopeType, boolean includeInactive, int page, int size) {
        if (page < 0 || size < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Invalid pagination parameters.");
        }
        Page<ThresholdRule> rules = thresholdRulePort.list(
                parseOptionalMetricKey(metricKey),
                parseOptionalScopeType(scopeType),
                includeInactive,
                PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("updatedAt").descending()));
        return new ThresholdRulePage(
                rules.getContent(),
                rules.getNumber(),
                rules.getSize(),
                rules.getTotalElements(),
                rules.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ThresholdRule get(UUID id) {
        return thresholdRulePort.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public ThresholdRule create(AuthenticatedPrincipal actor, ThresholdRuleCommand command) {
        ThresholdRuleDraft draft = draft(actor, command);
        authorizeWrite(actor, draft.metricKey());
        ensureScopeExists(draft.scopeType(), draft.scopeId());
        ensureDefinitionAvailable(draft, null);
        ThresholdRule created = thresholdRulePort.create(draft);
        appendAudit(actor, "CREATE", created);
        return created;
    }

    @Transactional
    public ThresholdRule update(AuthenticatedPrincipal actor, UUID id, ThresholdRuleCommand command) {
        get(id);
        ThresholdRuleDraft draft = draft(actor, command);
        authorizeWrite(actor, draft.metricKey());
        ensureScopeExists(draft.scopeType(), draft.scopeId());
        ensureDefinitionAvailable(draft, id);
        ThresholdRule updated = thresholdRulePort.update(id, draft);
        appendAudit(actor, "UPDATE", updated);
        return updated;
    }

    @Transactional
    public void delete(AuthenticatedPrincipal actor, UUID id) {
        ThresholdRule existing = get(id);
        authorizeWrite(actor, existing.metricKey());
        thresholdRulePort.setActive(id, false, actor.userId());
        appendAudit(actor, "DELETE", existing);
    }

    private ThresholdRuleDraft draft(AuthenticatedPrincipal actor, ThresholdRuleCommand command) {
        ThresholdMetricKey metricKey = parseMetricKey(command.metricKey());
        ThresholdScopeType scopeType = parseScopeType(command.scopeType());
        UUID scopeId = normalizeScopeId(scopeType, command.scopeId());
        BigDecimal minValue = command.minValue();
        BigDecimal maxValue = command.maxValue();
        if (minValue == null && maxValue == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "At least one threshold bound is required.");
        }
        if (minValue != null && maxValue != null && minValue.compareTo(maxValue) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "minValue must be less than or equal to maxValue.");
        }
        return new ThresholdRuleDraft(
                metricKey,
                scopeType,
                scopeId,
                minValue,
                maxValue,
                parseSeverity(command.severity()),
                command.active(),
                actor.userId());
    }

    private void authorizeWrite(AuthenticatedPrincipal actor, ThresholdMetricKey metricKey) {
        if (actor.roles().contains(ADMIN)) {
            return;
        }
        if (metricKey.isEnergyMetric()
                && actor.roles().contains("ENERGY_MANAGER")
                && systemSettingsPort.get().energyThresholdDelegationEnabled()) {
            return;
        }
        if (metricKey.isEnergyMetric()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    ErrorCode.ACCESS_DENIED,
                    "Energy threshold write access is not delegated.");
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Only ADMIN can manage non-energy thresholds.");
        }
    }

    private void ensureScopeExists(ThresholdScopeType scopeType, UUID scopeId) {
        if (scopeType == ThresholdScopeType.GLOBAL) {
            return;
        }
        MasterDataKind kind = switch (scopeType) {
            case FACTORY -> MasterDataKind.FACTORY;
            case LINE -> MasterDataKind.PRODUCTION_LINE;
            case MACHINE -> MasterDataKind.MACHINE;
            case PRODUCT -> MasterDataKind.PRODUCT;
            case SHIFT -> MasterDataKind.SHIFT;
            case GLOBAL -> throw new IllegalStateException("GLOBAL scope has no master-data kind.");
        };
        if (masterDataPort.findById(kind, scopeId).isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.BUSINESS_RULE_VIOLATION, "Threshold scope does not exist.");
        }
    }

    private void ensureDefinitionAvailable(ThresholdRuleDraft draft, UUID excludedId) {
        thresholdRulePort.findActiveByDefinition(draft, excludedId).ifPresent(existing -> {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Active threshold already exists for this metric, scope and severity.");
        });
    }

    private void appendAudit(AuthenticatedPrincipal actor, String action, ThresholdRule rule) {
        auditEventPort.append(
                AuditEventType.THRESHOLD_CHANGED,
                actor.userId(),
                null,
                "action=" + action
                        + ";thresholdId=" + rule.id()
                        + ";metricKey=" + rule.metricKey()
                        + ";scopeType=" + rule.scopeType()
                        + ";severity=" + rule.severity());
    }

    private static UUID normalizeScopeId(ThresholdScopeType scopeType, UUID scopeId) {
        if (scopeType == ThresholdScopeType.GLOBAL) {
            if (scopeId != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "GLOBAL thresholds must not define scopeId.");
            }
            return null;
        }
        if (scopeId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "scopeId is required for non-GLOBAL thresholds.");
        }
        return scopeId;
    }

    private static ThresholdMetricKey parseOptionalMetricKey(String value) {
        return value == null || value.isBlank() ? null : parseMetricKey(value);
    }

    private static ThresholdScopeType parseOptionalScopeType(String value) {
        return value == null || value.isBlank() ? null : parseScopeType(value);
    }

    private static ThresholdMetricKey parseMetricKey(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "metricKey is required.");
        }
        try {
            return ThresholdMetricKey.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Unsupported threshold metricKey.");
        }
    }

    private static ThresholdScopeType parseScopeType(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "scopeType is required.");
        }
        try {
            return ThresholdScopeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Unsupported threshold scopeType.");
        }
    }

    private static ThresholdSeverity parseSeverity(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "severity is required.");
        }
        try {
            return ThresholdSeverity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Unsupported threshold severity.");
        }
    }

    private static ApiException notFound(UUID id) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Threshold was not found: " + id);
    }

    public record ThresholdRuleCommand(
            String metricKey,
            String scopeType,
            UUID scopeId,
            BigDecimal minValue,
            BigDecimal maxValue,
            String severity,
            boolean active) {
    }

    public record ThresholdRulePage(
            List<ThresholdRule> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {

        public ThresholdRulePage {
            items = List.copyOf(items);
        }
    }
}
