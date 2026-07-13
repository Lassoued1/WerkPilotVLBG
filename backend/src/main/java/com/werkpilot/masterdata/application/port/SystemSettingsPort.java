package com.werkpilot.masterdata.application.port;

import java.util.UUID;

public interface SystemSettingsPort {

    SystemSettings get();

    SystemSettings setEnergyThresholdDelegationEnabled(boolean enabled, UUID actorUserId);
}
