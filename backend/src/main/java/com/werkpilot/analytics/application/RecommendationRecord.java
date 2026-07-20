package com.werkpilot.analytics.application;

import java.time.Instant;
import java.util.UUID;

public record RecommendationRecord(
        UUID id,
        UUID anomalyId,
        String templateCode,
        String templateVersion,
        String messageDe,
        String disclaimerDe,
        Instant createdAt) {
}
