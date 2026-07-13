package com.werkpilot.identity.persistence;

import com.werkpilot.identity.application.port.PasswordResetToken;
import com.werkpilot.identity.application.port.PasswordResetTokenPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenPort {

    private final PasswordResetTokenRepository repository;

    public PasswordResetTokenPersistenceAdapter(PasswordResetTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PasswordResetToken create(UUID userId, String tokenHash, Instant requestedAt, Instant expiresAt) {
        return toToken(repository.save(new PasswordResetTokenEntity(UUID.randomUUID(), userId, tokenHash, requestedAt, expiresAt)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toToken);
    }

    @Override
    @Transactional
    public void consume(UUID id, Instant consumedAt) {
        repository.findById(id).ifPresent(entity -> entity.consume(consumedAt));
    }

    private PasswordResetToken toToken(PasswordResetTokenEntity entity) {
        return new PasswordResetToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getRequestedAt(),
                entity.getExpiresAt(),
                entity.getConsumedAt());
    }
}
