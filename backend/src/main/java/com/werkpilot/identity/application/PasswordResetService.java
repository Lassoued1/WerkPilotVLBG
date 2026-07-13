package com.werkpilot.identity.application;

import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.identity.application.port.PasswordResetMailPort;
import com.werkpilot.identity.application.port.PasswordResetToken;
import com.werkpilot.identity.application.port.PasswordResetTokenPort;
import com.werkpilot.identity.application.port.RefreshSessionPort;
import com.werkpilot.identity.application.port.UserAccount;
import com.werkpilot.identity.application.port.UserAccountPort;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private final UserAccountPort userAccountPort;
    private final PasswordResetTokenPort passwordResetTokenPort;
    private final RefreshSessionPort refreshSessionPort;
    private final PasswordResetMailPort passwordResetMailPort;
    private final SecureTokenGenerator tokenGenerator;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventPort auditEventPort;
    private final Clock clock;
    private final Duration tokenTtl;
    private final String appBaseUrl;

    public PasswordResetService(
            UserAccountPort userAccountPort,
            PasswordResetTokenPort passwordResetTokenPort,
            RefreshSessionPort refreshSessionPort,
            PasswordResetMailPort passwordResetMailPort,
            SecureTokenGenerator tokenGenerator,
            PasswordEncoder passwordEncoder,
            AuditEventPort auditEventPort,
            Clock clock,
            @Value("${werkpilot.identity.password-reset-token-ttl}") Duration tokenTtl,
            @Value("${werkpilot.identity.app-base-url}") String appBaseUrl) {
        this.userAccountPort = userAccountPort;
        this.passwordResetTokenPort = passwordResetTokenPort;
        this.refreshSessionPort = refreshSessionPort;
        this.passwordResetMailPort = passwordResetMailPort;
        this.tokenGenerator = tokenGenerator;
        this.passwordEncoder = passwordEncoder;
        this.auditEventPort = auditEventPort;
        this.clock = clock;
        this.tokenTtl = tokenTtl;
        this.appBaseUrl = appBaseUrl;
    }

    @Transactional
    public void requestReset(String email) {
        userAccountPort.findByEmail(normalizeEmail(email))
                .filter(UserAccount::active)
                .ifPresent(user -> issueResetLink(null, user));
    }

    @Transactional
    public void adminTriggerReset(AuthenticatedPrincipal actor, UUID userId) {
        UserAccount user = userAccountPort.findById(userId).orElseThrow(() -> notFound(userId));
        if (user.active()) {
            issueResetLink(actor.userId(), user);
        }
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        Instant now = clock.instant();
        PasswordResetToken resetToken = passwordResetTokenPort.findByTokenHash(tokenGenerator.sha256(token))
                .filter(candidate -> candidate.isActiveAt(now))
                .orElseThrow(this::invalidToken);
        UserAccount user = userAccountPort.findById(resetToken.userId())
                .filter(UserAccount::active)
                .orElseThrow(this::invalidToken);

        userAccountPort.updatePassword(user.id(), passwordEncoder.encode(newPassword));
        passwordResetTokenPort.consume(resetToken.id(), now);
        refreshSessionPort.revokeAllForUser(user.id(), now);
        auditEventPort.append(AuditEventType.PASSWORD_RESET_COMPLETED, user.id(), user.id(), "method=email");
    }

    private void issueResetLink(UUID actorUserId, UserAccount user) {
        String token = tokenGenerator.randomToken();
        Instant now = clock.instant();
        PasswordResetToken resetToken = passwordResetTokenPort.create(
                user.id(),
                tokenGenerator.sha256(token),
                now,
                now.plus(tokenTtl));
        passwordResetMailPort.sendPasswordResetMail(user.email(), user.displayName(), resetLink(token));
        auditEventPort.append(
                AuditEventType.PASSWORD_RESET_REQUESTED,
                actorUserId,
                user.id(),
                "expiresAt=" + resetToken.expiresAt());
    }

    private String resetLink(String token) {
        String normalizedBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
        return normalizedBaseUrl + "/password-reset#token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private ApiException invalidToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_TOKEN_EXPIRED, "Password reset token is invalid or expired.");
    }

    private static ApiException notFound(UUID id) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "User was not found: " + id);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
