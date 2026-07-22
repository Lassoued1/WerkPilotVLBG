package com.werkpilot.maintenance.application;

import com.werkpilot.analytics.application.AnomalyDetectionCandidate;
import com.werkpilot.analytics.application.AnomalyPort;
import com.werkpilot.analytics.application.RecommendationPort;
import com.werkpilot.analytics.application.RecommendationService;
import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.analytics.domain.BaselineQuality;
import com.werkpilot.analytics.domain.DetectionMethod;
import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecurringTicketPatternService {

    private static final int RECURRENCE_THRESHOLD = 3;
    private static final String DETECTOR_VERSION = "WP-S4-RECURRING-TICKET-V1";

    private final MaintenanceTicketPort ticketPort;
    private final AnomalyPort anomalyPort;
    private final RecommendationService recommendationService;
    private final RecommendationPort recommendationPort;
    private final Clock clock;

    RecurringTicketPatternService(
            MaintenanceTicketPort ticketPort,
            AnomalyPort anomalyPort,
            RecommendationService recommendationService,
            RecommendationPort recommendationPort) {
        this(ticketPort, anomalyPort, recommendationService, recommendationPort, Clock.systemUTC());
    }

    RecurringTicketPatternService(
            MaintenanceTicketPort ticketPort,
            AnomalyPort anomalyPort,
            RecommendationService recommendationService,
            RecommendationPort recommendationPort,
            Clock clock) {
        this.ticketPort = ticketPort;
        this.anomalyPort = anomalyPort;
        this.recommendationService = recommendationService;
        this.recommendationPort = recommendationPort;
        this.clock = clock;
    }

    void detectIfRecurring(MaintenanceTicket ticket) {
        if (ticket.machineId() == null || ticket.issueCategory() == null) {
            return;
        }
        Instant windowEnd = clock.instant();
        Instant windowStart = windowEnd.minus(30, ChronoUnit.DAYS);
        List<MaintenanceTicket> relatedTickets = ticketPort.findByMachineAndIssueCategoryCreatedSince(
                ticket.machineId(), ticket.issueCategory(), windowStart);
        if (relatedTickets.size() < RECURRENCE_THRESHOLD) {
            return;
        }

        String identityKey = "%s|machine=%s|category=%s".formatted(DETECTOR_VERSION, ticket.machineId(), ticket.issueCategory());
        if (anomalyPort.findActiveByIdentityKey(identityKey).isPresent()) {
            return;
        }

        AnomalyDetectionCandidate candidate = new AnomalyDetectionCandidate(
                identityKey,
                DETECTOR_VERSION,
                ThresholdMetricKey.DOWNTIME_MINUTES,
                AnomalyType.RECURRING_TICKET_PATTERN,
                ThresholdSeverity.CRITICAL,
                DetectionMethod.THRESHOLD,
                ticket.factoryId(),
                ticket.lineId(),
                ticket.machineId(),
                null,
                null,
                windowStart,
                windowEnd,
                BigDecimal.valueOf(relatedTickets.size()),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                relatedTickets.size(),
                BaselineQuality.LOW,
                null,
                null,
                "Wiederkehrendes Ticketmuster: %d Tickets der Kategorie %s an derselben Maschine innerhalb von 30 Tagen."
                        .formatted(relatedTickets.size(), ticket.issueCategory()),
                "recurring-ticket|%s|%s|%d".formatted(ticket.machineId(), ticket.issueCategory(), relatedTickets.size()));
        var anomaly = anomalyPort.create(candidate, null);
        recommendationPort.replaceForAnomaly(anomaly.id(), recommendationService.recommendationsFor(anomaly.id(), candidate));
    }
}
