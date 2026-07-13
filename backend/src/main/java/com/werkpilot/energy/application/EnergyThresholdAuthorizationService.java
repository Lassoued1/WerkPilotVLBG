package com.werkpilot.energy.application;

import com.werkpilot.masterdata.application.port.SystemSettingsPort;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnergyThresholdAuthorizationService {

    private static final String ADMIN = "ADMIN";
    private static final String ENERGY_MANAGER = "ENERGY_MANAGER";

    private final SystemSettingsPort systemSettingsPort;

    public EnergyThresholdAuthorizationService(SystemSettingsPort systemSettingsPort) {
        this.systemSettingsPort = systemSettingsPort;
    }

    @Transactional(readOnly = true)
    public void assertCanWriteEnergyThreshold(AuthenticatedPrincipal principal) {
        if (principal.roles().contains(ADMIN)) {
            return;
        }
        if (principal.roles().contains(ENERGY_MANAGER)
                && systemSettingsPort.get().energyThresholdDelegationEnabled()) {
            return;
        }
        throw new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.ACCESS_DENIED,
                "Energy threshold write access is not delegated.");
    }
}
