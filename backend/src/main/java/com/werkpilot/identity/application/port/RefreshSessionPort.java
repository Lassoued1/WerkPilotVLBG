package com.werkpilot.identity.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionPort {

    RefreshSession create(UUID userId, String tokenHash, String csrfTokenHash, Instant issuedAt, Instant expiresAt);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    void revoke(UUID id, Instant revokedAt);

    void revokeAllForUser(UUID userId, Instant revokedAt);

    void replace(UUID oldSessionId, UUID newSessionId, Instant replacedAt);
}
