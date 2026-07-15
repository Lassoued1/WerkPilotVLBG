package com.werkpilot.downtime.application.port;

import java.time.Instant;
import java.util.UUID;

public record DowntimeRecordDraft(
        UUID id,
        UUID importJobId,
        Instant periodStart,
        Instant periodEnd,
        UUID machineId,
        UUID shiftId,
        int downtimeMin,
        UUID reasonId,
        String comment) {
}
