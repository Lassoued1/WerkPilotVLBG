package com.werkpilot.identity.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenPort {

    PasswordResetToken create(UUID userId, String tokenHash, Instant requestedAt, Instant expiresAt);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void consume(UUID id, Instant consumedAt);
}
