package com.werkpilot.analytics.api;

import com.werkpilot.analytics.application.ThresholdAdministrationService;
import com.werkpilot.analytics.application.ThresholdAdministrationService.ThresholdRuleCommand;
import com.werkpilot.analytics.application.ThresholdAdministrationService.ThresholdRulePage;
import com.werkpilot.analytics.application.ThresholdRule;
import com.werkpilot.shared.api.PageResponse;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ThresholdAdministrationController {

    private final ThresholdAdministrationService thresholdAdministrationService;

    ThresholdAdministrationController(ThresholdAdministrationService thresholdAdministrationService) {
        this.thresholdAdministrationService = thresholdAdministrationService;
    }

    @GetMapping("/thresholds")
    PageResponse<ThresholdRuleResponse> list(
            @RequestParam(required = false) String metricKey,
            @RequestParam(required = false) String scopeType,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ThresholdRulePage rules = thresholdAdministrationService.list(metricKey, scopeType, includeInactive, page, size);
        return new PageResponse<>(
                rules.items().stream().map(ThresholdAdministrationController::response).toList(),
                rules.page(),
                rules.size(),
                rules.totalElements(),
                rules.totalPages());
    }

    @GetMapping("/thresholds/{id}")
    ThresholdRuleResponse get(@PathVariable UUID id) {
        return response(thresholdAdministrationService.get(id));
    }

    @PostMapping("/thresholds")
    ResponseEntity<ThresholdRuleResponse> create(
            Authentication authentication,
            @Valid @RequestBody ThresholdRuleRequest request) {
        ThresholdRuleResponse created = response(thresholdAdministrationService.create(principal(authentication), command(request)));
        return ResponseEntity.created(URI.create("/thresholds/" + created.id())).body(created);
    }

    @PutMapping("/thresholds/{id}")
    ThresholdRuleResponse update(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody ThresholdRuleRequest request) {
        return response(thresholdAdministrationService.update(principal(authentication), id, command(request)));
    }

    @DeleteMapping("/thresholds/{id}")
    ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID id) {
        thresholdAdministrationService.delete(principal(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private static AuthenticatedPrincipal principal(Authentication authentication) {
        return (AuthenticatedPrincipal) authentication.getPrincipal();
    }

    private static ThresholdRuleCommand command(ThresholdRuleRequest request) {
        return new ThresholdRuleCommand(
                request.metricKey(),
                request.scopeType(),
                request.scopeId(),
                request.minValue(),
                request.maxValue(),
                request.severity(),
                request.active());
    }

    private static ThresholdRuleResponse response(ThresholdRule rule) {
        return new ThresholdRuleResponse(
                rule.id(),
                rule.metricKeyValue(),
                rule.scopeTypeValue(),
                rule.scopeId(),
                rule.minValue(),
                rule.maxValue(),
                rule.severityValue(),
                rule.active(),
                rule.createdByUserId(),
                rule.updatedByUserId(),
                rule.createdAt(),
                rule.updatedAt());
    }

    public record ThresholdRuleRequest(
            @Size(max = 64) String metricKey,
            @Size(max = 20) String scopeType,
            UUID scopeId,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal minValue,
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal maxValue,
            @Size(max = 20) String severity,
            boolean active) {
    }

    public record ThresholdRuleResponse(
            UUID id,
            String metricKey,
            String scopeType,
            UUID scopeId,
            BigDecimal minValue,
            BigDecimal maxValue,
            String severity,
            boolean active,
            UUID createdByUserId,
            UUID updatedByUserId,
            Instant createdAt,
            Instant updatedAt) {
    }
}
