package com.werkpilot.masterdata.application;

import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.masterdata.application.port.SystemSettings;
import com.werkpilot.masterdata.application.port.SystemSettingsPort;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemSettingsService {

    private final SystemSettingsPort systemSettingsPort;
    private final AuditEventPort auditEventPort;

    public SystemSettingsService(SystemSettingsPort systemSettingsPort, AuditEventPort auditEventPort) {
        this.systemSettingsPort = systemSettingsPort;
        this.auditEventPort = auditEventPort;
    }

    @Transactional(readOnly = true)
    public SystemSettings getSettings() {
        return systemSettingsPort.get();
    }

    @Transactional
    public SystemSettings setEnergyThresholdDelegationEnabled(AuthenticatedPrincipal actor, boolean enabled) {
        SystemSettings before = systemSettingsPort.get();
        SystemSettings after = systemSettingsPort.setEnergyThresholdDelegationEnabled(enabled, actor.userId());
        if (before.energyThresholdDelegationEnabled() != after.energyThresholdDelegationEnabled()) {
            auditEventPort.append(
                    AuditEventType.THRESHOLD_DELEGATION_CHANGED,
                    actor.userId(),
                    null,
                    "from=" + before.energyThresholdDelegationEnabled() + ";to=" + after.energyThresholdDelegationEnabled());
        }
        return after;
    }
}
