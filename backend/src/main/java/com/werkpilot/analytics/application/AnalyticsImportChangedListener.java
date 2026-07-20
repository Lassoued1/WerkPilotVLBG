package com.werkpilot.analytics.application;

import com.werkpilot.shared.events.ImportAnalyticsChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class AnalyticsImportChangedListener {

    private final AnalyticsImportWindowPort importWindowPort;
    private final AnomalyRerunService anomalyRerunService;

    AnalyticsImportChangedListener(AnalyticsImportWindowPort importWindowPort, AnomalyRerunService anomalyRerunService) {
        this.importWindowPort = importWindowPort;
        this.anomalyRerunService = anomalyRerunService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void rerunAnalyticsForCommittedImport(ImportAnalyticsChangedEvent event) {
        importWindowPort.findWindow(event.importJobId(), event.importType())
                .filter(window -> window.from().isBefore(window.to()))
                .ifPresent(window -> anomalyRerunService.rerun(new KpiQuery(window.from(), window.to(), null, null, null, null, null)));
    }
}
