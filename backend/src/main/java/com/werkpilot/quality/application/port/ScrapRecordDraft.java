package com.werkpilot.quality.application.port;

import java.time.Instant;
import java.util.UUID;

public record ScrapRecordDraft(
        UUID id,
        UUID importJobId,
        Instant periodStart,
        Instant periodEnd,
        UUID machineId,
        UUID productId,
        UUID shiftId,
        int scrapCount,
        UUID scrapCategoryId) {
}
