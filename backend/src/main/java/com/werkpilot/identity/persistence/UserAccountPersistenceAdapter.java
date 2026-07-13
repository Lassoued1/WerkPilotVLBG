package com.werkpilot.identity.persistence;

import com.werkpilot.identity.application.port.UserAccount;
import com.werkpilot.identity.application.port.UserAccountPort;
import com.werkpilot.identity.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserAccountPersistenceAdapter implements UserAccountPort {

    private final UserAccountRepository repository;
    private final Clock clock;

    public UserAccountPersistenceAdapter(UserAccountRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(id).map(this::toAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserAccount> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toAccount);
    }

    @Override
    @Transactional
    public UserAccount createUser(String email, String displayName, String passwordHash, Set<UserRole> roles) {
        Instant now = clock.instant();
        UserAccountEntity entity = new UserAccountEntity(UUID.randomUUID(), email, displayName, passwordHash, roles, now);
        return toAccount(repository.save(entity));
    }

    @Override
    @Transactional
    public UserAccount updateUser(UUID id, String displayName, boolean active, Set<UserRole> roles) {
        UserAccountEntity entity = repository.getReferenceById(id);
        entity.update(displayName, active, roles, clock.instant());
        return toAccount(repository.save(entity));
    }

    @Override
    @Transactional
    public void updatePassword(UUID id, String passwordHash) {
        UserAccountEntity entity = repository.getReferenceById(id);
        entity.updatePassword(passwordHash, clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveAdminsExcluding(UUID excludedUserId) {
        return repository.countActiveUsersWithRoleExcluding(UserRole.ADMIN, excludedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<UserAccount> findActiveAdmins() {
        return repository.findActiveUsersWithRole(UserRole.ADMIN).stream()
                .map(this::toAccount)
                .toList();
    }

    private UserAccount toAccount(UserAccountEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPasswordHash(),
                entity.isActive(),
                entity.getRoles());
    }
}
