package com.werkpilot.analytics.application;

import java.util.Optional;
import java.util.UUID;

public interface AnalyticsImportWindowPort {

    Optional<AnalyticsImportWindow> findWindow(UUID importJobId, String importType);
}
