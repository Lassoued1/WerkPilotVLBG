package com.werkpilot.importing.application.port;

import com.werkpilot.importing.domain.ImportJobStatus;
import com.werkpilot.importing.domain.ImportType;
import java.time.Instant;
import java.util.UUID;

public record ImportJobRecord(
        UUID id,
        ImportType importType,
        ImportJobStatus status,
        String originalFilename,
        String safeFilename,
        String fileHashSha256,
        long fileSizeBytes,
        int totalRows,
        int validRows,
        int errorCount,
        boolean errorOverflow,
        UUID correctsImportJobId,
        UUID createdByUserId,
        Instant createdAt,
        Instant completedAt,
        String failureReason) {
}
