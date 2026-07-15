package com.werkpilot.importing.application.port;

import java.time.Instant;
import java.util.UUID;

public record ImportJobErrorRecord(
        UUID id,
        UUID importJobId,
        int rowNumber,
        String columnName,
        String rejectedValue,
        String message,
        Instant createdAt) {
}
