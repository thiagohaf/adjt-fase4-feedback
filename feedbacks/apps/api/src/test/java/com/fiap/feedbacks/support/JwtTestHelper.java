package com.fiap.feedbacks.support;

import io.smallrye.jwt.build.Jwt;
import org.jose4j.keys.HmacKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public final class JwtTestHelper {

    private JwtTestHelper() {
    }

    public static String createExpiredToken(String secret, UUID userId, String role) {
        Instant past = Instant.now().minusSeconds(3600);
        return Jwt.claims()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(past.minusSeconds(7200))
                .expiresAt(past)
                .jws()
                .sign(new HmacKey(secret.getBytes(StandardCharsets.UTF_8)));
    }

    public static String createTokenWithInvalidSignature(UUID userId, String role) {
        String wrongSecret = "wrong-secret-min-256-bits-for-hs256-demo-key!!";
        Instant now = Instant.now();
        return Jwt.claims()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .jws()
                .sign(new HmacKey(wrongSecret.getBytes(StandardCharsets.UTF_8)));
    }
}
