package com.werkpilot.identity.application.port;

import com.werkpilot.identity.domain.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAccountPort {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(UUID id);

    Page<UserAccount> findAll(Pageable pageable);

    UserAccount createUser(String email, String displayName, String passwordHash, Set<UserRole> roles);

    UserAccount updateUser(UUID id, String displayName, boolean active, Set<UserRole> roles);

    void updatePassword(UUID id, String passwordHash);

    long countActiveAdminsExcluding(UUID excludedUserId);

    List<UserAccount> findActiveAdmins();
}
