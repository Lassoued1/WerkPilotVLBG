package com.werkpilot.shared.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenCodec {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final Duration ttl;
    private final Clock clock;

    public AccessTokenCodec(
            @Value("${werkpilot.security.access-token-secret}") String secret,
            @Value("${werkpilot.security.access-token-ttl}") Duration ttl,
            Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl;
        this.clock = clock;
    }

    public IssuedAccessToken issue(UUID userId, String email, List<String> roles) {
        Instant expiresAt = clock.instant().plus(ttl);
        String payload = String.join("|",
                userId.toString(),
                email,
                Long.toString(expiresAt.getEpochSecond()),
                UUID.randomUUID().toString(),
                String.join(",", roles));
        String encodedPayload = ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = ENCODER.encodeToString(hmac(encodedPayload));
        return new IssuedAccessToken(encodedPayload + "." + signature, expiresAt);
    }

    public Optional<AuthenticatedPrincipal> verify(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2 || !constantTimeEquals(parts[1], ENCODER.encodeToString(hmac(parts[0])))) {
                return Optional.empty();
            }

            String payload = new String(DECODER.decode(parts[0]), StandardCharsets.UTF_8);
            String[] fields = payload.split("\\|", -1);
            if (fields.length != 5) {
                return Optional.empty();
            }

            Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(fields[2]));
            if (!expiresAt.isAfter(clock.instant())) {
                return Optional.empty();
            }

            List<String> roles = fields[4].isBlank()
                    ? List.of()
                    : Arrays.stream(fields[4].split(",")).toList();
            return Optional.of(new AuthenticatedPrincipal(UUID.fromString(fields[0]), fields[1], roles));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot sign access token", ex);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    public record IssuedAccessToken(String token, Instant expiresAt) {
    }
}
