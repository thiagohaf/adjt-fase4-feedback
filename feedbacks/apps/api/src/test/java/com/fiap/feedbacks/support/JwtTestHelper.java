package com.fiap.feedbacks.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class JwtTestHelper {

    private JwtTestHelper() {
    }

    public static String createExpiredToken(String secret, UUID userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        var past = Instant.now().minusSeconds(3600);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(past.minusSeconds(7200)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();
    }

    public static String createTokenWithInvalidSignature(UUID userId, String role) {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-min-256-bits-for-hs256-demo-key!!".getBytes(StandardCharsets.UTF_8)
        );
        var now = Instant.now();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(wrongKey)
                .compact();
    }
}
