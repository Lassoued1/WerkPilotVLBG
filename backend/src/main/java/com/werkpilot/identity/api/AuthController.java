package com.werkpilot.identity.api;

import com.werkpilot.identity.application.AuthResponses.AccessTokenResponse;
import com.werkpilot.identity.application.AuthResponses.AuthenticatedSession;
import com.werkpilot.identity.application.AuthResponses.UserProfile;
import com.werkpilot.identity.application.AuthenticationService;
import com.werkpilot.identity.application.PasswordResetService;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;
    private final String refreshCookieName;
    private final String csrfHeaderName;

    public AuthController(
            AuthenticationService authenticationService,
            PasswordResetService passwordResetService,
            @Value("${werkpilot.security.refresh-cookie-name}") String refreshCookieName,
            @Value("${werkpilot.security.csrf-header-name}") String csrfHeaderName) {
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
        this.refreshCookieName = refreshCookieName;
        this.csrfHeaderName = csrfHeaderName;
    }

    @PostMapping("/login")
    ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedSession session = authenticationService.login(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(toTokenResponse(session));
    }

    @PostMapping("/refresh")
    ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request) {
        AuthenticatedSession session = authenticationService.refresh(refreshCookieValue(request), csrfHeaderValue(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(toTokenResponse(session));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        authenticationService.logout(refreshCookieValue(request), csrfHeaderValue(request));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    UserProfile me(Authentication authentication) {
        return authenticationService.currentProfile((AuthenticatedPrincipal) authentication.getPrincipal());
    }

    @PostMapping("/password-reset-request")
    ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/password-reset-confirm")
    ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private AccessTokenResponse toTokenResponse(AuthenticatedSession session) {
        return new AccessTokenResponse(
                session.accessToken(),
                session.accessTokenExpiresAt(),
                session.csrfToken(),
                session.profile());
    }

    private ResponseCookie refreshCookie(AuthenticatedSession session) {
        return ResponseCookie.from(refreshCookieName, session.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(Duration.between(java.time.Instant.now(), session.refreshTokenExpiresAt()))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    private String refreshCookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw invalidRefreshRequest();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> refreshCookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(this::invalidRefreshRequest);
    }

    private String csrfHeaderValue(HttpServletRequest request) {
        String value = request.getHeader(csrfHeaderName);
        if (value == null || value.isBlank()) {
            throw invalidRefreshRequest();
        }
        return value;
    }

    private ApiException invalidRefreshRequest() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_TOKEN_EXPIRED, "Refresh token is invalid or expired.");
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record PasswordResetRequest(@NotBlank @Email String email) {
    }

    public record PasswordResetConfirmRequest(
            @NotBlank String token,
            @NotBlank @jakarta.validation.constraints.Size(min = 12, max = 128) String newPassword) {
    }
}
