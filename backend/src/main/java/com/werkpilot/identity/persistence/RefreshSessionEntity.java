package com.werkpilot.identity.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_session")
public class RefreshSessionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 64)
    private String csrfTokenHash;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    private UUID replacedById;

    protected RefreshSessionEntity() {
    }

    RefreshSessionEntity(UUID id, UUID userId, String tokenHash, String csrfTokenHash, Instant issuedAt, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.csrfTokenHash = csrfTokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    String getTokenHash() {
        return tokenHash;
    }

    String getCsrfTokenHash() {
        return csrfTokenHash;
    }

    Instant getIssuedAt() {
        return issuedAt;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }

    Instant getRevokedAt() {
        return revokedAt;
    }

    UUID getReplacedById() {
        return replacedById;
    }

    void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    void replace(UUID replacedById, Instant replacedAt) {
        this.replacedById = replacedById;
        this.revokedAt = replacedAt;
    }
}
