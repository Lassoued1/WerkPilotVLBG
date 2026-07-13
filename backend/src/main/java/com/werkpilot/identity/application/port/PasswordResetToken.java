package com.werkpilot.identity.application.port;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetToken(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant requestedAt,
        Instant expiresAt,
        Instant consumedAt) {

    public boolean isActiveAt(Instant instant) {
        return consumedAt == null && expiresAt.isAfter(instant);
    }
}
