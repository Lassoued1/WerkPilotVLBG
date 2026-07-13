package com.werkpilot.shared.security;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID userId,
        String email,
        List<String> roles) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
