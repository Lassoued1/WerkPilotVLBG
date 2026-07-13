package com.werkpilot.identity.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AuthResponses {

    private AuthResponses() {
    }

    public record AuthenticatedSession(
            String accessToken,
            Instant accessTokenExpiresAt,
            String csrfToken,
            String refreshToken,
            Instant refreshTokenExpiresAt,
            UserProfile profile) {
    }

    public record AccessTokenResponse(
            String accessToken,
            Instant accessTokenExpiresAt,
            String csrfToken,
            UserProfile profile) {
    }

    public record UserProfile(
            UUID id,
            String email,
            String displayName,
            List<String> roles) {
    }
}
