package com.werkpilot.identity.persistence;

import com.werkpilot.identity.application.port.RefreshSession;
import com.werkpilot.identity.application.port.RefreshSessionPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshSessionPersistenceAdapter implements RefreshSessionPort {

    private final RefreshSessionRepository repository;

    public RefreshSessionPersistenceAdapter(RefreshSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public RefreshSession create(UUID userId, String tokenHash, String csrfTokenHash, Instant issuedAt, Instant expiresAt) {
        RefreshSessionEntity entity = new RefreshSessionEntity(UUID.randomUUID(), userId, tokenHash, csrfTokenHash, issuedAt, expiresAt);
        return toSession(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toSession);
    }

    @Override
    @Transactional
    public void revoke(UUID id, Instant revokedAt) {
        repository.findById(id).ifPresent(entity -> entity.revoke(revokedAt));
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId, Instant revokedAt) {
        repository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(entity -> entity.revoke(revokedAt));
    }

    @Override
    @Transactional
    public void replace(UUID oldSessionId, UUID newSessionId, Instant replacedAt) {
        repository.findById(oldSessionId).ifPresent(entity -> entity.replace(newSessionId, replacedAt));
    }

    private RefreshSession toSession(RefreshSessionEntity entity) {
        return new RefreshSession(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getCsrfTokenHash(),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getReplacedById());
    }
}
