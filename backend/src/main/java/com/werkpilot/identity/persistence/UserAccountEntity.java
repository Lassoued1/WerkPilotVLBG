package com.werkpilot.identity.persistence;

import com.werkpilot.identity.domain.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserAccountEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 64)
    private Set<UserRole> roles = new LinkedHashSet<>();

    protected UserAccountEntity() {
    }

    UserAccountEntity(UUID id, String email, String displayName, String passwordHash, Set<UserRole> roles, Instant now) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
        this.roles = new LinkedHashSet<>(roles);
    }

    UUID getId() {
        return id;
    }

    String getEmail() {
        return email;
    }

    String getDisplayName() {
        return displayName;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    boolean isActive() {
        return active;
    }

    Set<UserRole> getRoles() {
        return Set.copyOf(roles);
    }

    void update(String displayName, boolean active, Set<UserRole> roles, Instant updatedAt) {
        this.displayName = displayName;
        this.active = active;
        this.roles = new LinkedHashSet<>(roles);
        this.updatedAt = updatedAt;
    }

    void updatePassword(String passwordHash, Instant updatedAt) {
        this.passwordHash = passwordHash;
        this.updatedAt = updatedAt;
    }
}
