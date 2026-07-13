package com.werkpilot.masterdata.api;

import com.werkpilot.masterdata.application.SystemSettingsService;
import com.werkpilot.masterdata.application.port.SystemSettings;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/settings/global")
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    public SystemSettingsController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @GetMapping
    SystemSettingsResponse getSettings() {
        return response(systemSettingsService.getSettings());
    }

    @PutMapping("/energy-threshold-delegation")
    SystemSettingsResponse setEnergyThresholdDelegation(
            Authentication authentication,
            @Valid @RequestBody EnergyThresholdDelegationRequest request) {
        return response(systemSettingsService.setEnergyThresholdDelegationEnabled(
                principal(authentication),
                request.enabled()));
    }

    private static AuthenticatedPrincipal principal(Authentication authentication) {
        return (AuthenticatedPrincipal) authentication.getPrincipal();
    }

    private static SystemSettingsResponse response(SystemSettings settings) {
        return new SystemSettingsResponse(
                settings.energyThresholdDelegationEnabled(),
                settings.updatedByUserId(),
                settings.createdAt(),
                settings.updatedAt());
    }

    public record EnergyThresholdDelegationRequest(boolean enabled) {
    }

    public record SystemSettingsResponse(
            boolean energyThresholdDelegationEnabled,
            UUID updatedByUserId,
            Instant createdAt,
            Instant updatedAt) {
    }
}
