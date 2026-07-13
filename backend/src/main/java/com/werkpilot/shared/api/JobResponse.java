package com.werkpilot.shared.api;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID jobId,
        JobStatus status,
        Instant createdAt,
        Instant completedAt) {

    public JobResponse {
        if (jobId == null) {
            throw new IllegalArgumentException("jobId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }
}
