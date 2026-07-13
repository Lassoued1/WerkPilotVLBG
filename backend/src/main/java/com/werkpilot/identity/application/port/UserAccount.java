package com.werkpilot.identity.application.port;

import com.werkpilot.identity.domain.UserRole;
import java.util.Set;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String email,
        String displayName,
        String passwordHash,
        boolean active,
        Set<UserRole> roles) {
}
