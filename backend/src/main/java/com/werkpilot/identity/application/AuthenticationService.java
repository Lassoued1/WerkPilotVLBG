package com.werkpilot.identity.application;

import com.werkpilot.identity.application.AuthResponses.AuthenticatedSession;
import com.werkpilot.identity.application.AuthResponses.UserProfile;
import com.werkpilot.identity.application.port.RefreshSession;
import com.werkpilot.identity.application.port.RefreshSessionPort;
import com.werkpilot.identity.application.port.UserAccount;
import com.werkpilot.identity.application.port.UserAccountPort;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AccessTokenCodec;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserAccountPort userAccountPort;
    private final RefreshSessionPort refreshSessionPort;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenCodec accessTokenCodec;
    private final SecureTokenGenerator tokenGenerator;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public AuthenticationService(
            UserAccountPort userAccountPort,
            RefreshSessionPort refreshSessionPort,
            PasswordEncoder passwordEncoder,
            AccessTokenCodec accessTokenCodec,
            SecureTokenGenerator tokenGenerator,
            Clock clock,
            @Value("${werkpilot.security.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.userAccountPort = userAccountPort;
        this.refreshSessionPort = refreshSessionPort;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenCodec = accessTokenCodec;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public AuthenticatedSession login(String email, String password) {
        UserAccount user = userAccountPort.findByEmail(normalizeEmail(email))
                .filter(UserAccount::active)
                .filter(candidate -> passwordEncoder.matches(password, candidate.passwordHash()))
                .orElseThrow(this::invalidCredentials);
        return createSession(user).authenticatedSession();
    }

    @Transactional
    public AuthenticatedSession refresh(String refreshToken, String csrfToken) {
        String tokenHash = tokenGenerator.sha256(refreshToken);
        RefreshSession current = refreshSessionPort.findByTokenHash(tokenHash)
                .filter(session -> session.isActiveAt(clock.instant()))
                .filter(session -> constantHashMatches(session.csrfTokenHash(), csrfToken))
                .orElseThrow(this::invalidRefreshToken);

        UserAccount user = userAccountPort.findById(current.userId())
                .filter(UserAccount::active)
                .orElseThrow(this::invalidRefreshToken);
        SessionCreation replacement = createSession(user);
        refreshSessionPort.replace(current.id(), replacement.refreshSession().id(), clock.instant());
        return replacement.authenticatedSession();
    }

    @Transactional
    public void logout(String refreshToken, String csrfToken) {
        String tokenHash = tokenGenerator.sha256(refreshToken);
        RefreshSession current = refreshSessionPort.findByTokenHash(tokenHash)
                .filter(session -> session.isActiveAt(clock.instant()))
                .filter(session -> constantHashMatches(session.csrfTokenHash(), csrfToken))
                .orElseThrow(this::invalidRefreshToken);
        refreshSessionPort.revoke(current.id(), clock.instant());
    }

    @Transactional(readOnly = true)
    public UserProfile currentProfile(AuthenticatedPrincipal principal) {
        UserAccount user = userAccountPort.findById(principal.userId())
                .filter(UserAccount::active)
                .orElseThrow(this::invalidCredentials);
        return toProfile(user);
    }

    private SessionCreation createSession(UserAccount user) {
        AccessTokenCodec.IssuedAccessToken accessToken = accessTokenCodec.issue(user.id(), user.email(), roleNames(user));
        String refreshToken = tokenGenerator.randomToken();
        String csrfToken = tokenGenerator.randomToken();
        Instant issuedAt = clock.instant();
        Instant refreshExpiresAt = issuedAt.plus(refreshTokenTtl);
        RefreshSession refreshSession = refreshSessionPort.create(
                user.id(),
                tokenGenerator.sha256(refreshToken),
                tokenGenerator.sha256(csrfToken),
                issuedAt,
                refreshExpiresAt);
        AuthenticatedSession authenticatedSession = new AuthenticatedSession(
                accessToken.token(),
                accessToken.expiresAt(),
                csrfToken,
                refreshToken,
                refreshExpiresAt,
                toProfile(user));
        return new SessionCreation(authenticatedSession, refreshSession);
    }

    private boolean constantHashMatches(String expectedHash, String token) {
        return java.security.MessageDigest.isEqual(
                expectedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                tokenGenerator.sha256(token).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private UserProfile toProfile(UserAccount user) {
        return new UserProfile(user.id(), user.email(), user.displayName(), roleNames(user));
    }

    private List<String> roleNames(UserAccount user) {
        return user.roles().stream().map(Enum::name).sorted().toList();
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password.");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_TOKEN_EXPIRED, "Refresh token is invalid or expired.");
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record SessionCreation(AuthenticatedSession authenticatedSession, RefreshSession refreshSession) {
    }
}
