package com.fiap.feedbacks.infrastructure.security;

import com.fiap.feedbacks.application.auth.dto.AuthenticatedUser;
import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.domain.exception.InvalidTokenException;
import com.fiap.feedbacks.domain.exception.TokenExpiredException;
import io.smallrye.jwt.build.Jwt;
import org.jose4j.keys.HmacKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmallRyeJwtTokenServiceTest {

    private static final String SECRET = "test-secret-min-256-bits-for-hs256-demo-key";
    private static final UUID USER_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    SmallRyeJwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new SmallRyeJwtTokenService(new JwtProperties() {
            @Override
            public String secret() {
                return SECRET;
            }

            @Override
            public Duration expiration() {
                return Duration.ofHours(1);
            }
        });
    }

    @Test
    void roundTrip_generateAndParse() {
        var user = new AuthenticatedUser(USER_ID, "estudante@demo.fiap", Papel.ESTUDANTE);
        String token = tokenService.generate(user);

        var parsed = tokenService.parse(token);

        assertThat(parsed.id()).isEqualTo(USER_ID);
        assertThat(parsed.papel()).isEqualTo(Papel.ESTUDANTE);
        assertThat(tokenService.expiresInSeconds()).isEqualTo(3600L);
    }

    @Test
    void expiredToken_throwsTokenExpired() {
        Instant past = Instant.now().minusSeconds(3600);
        String expired = Jwt.claims()
                .subject(USER_ID.toString())
                .claim("role", "ESTUDANTE")
                .issuedAt(past.minusSeconds(7200))
                .expiresAt(past)
                .jws()
                .sign(new HmacKey(SECRET.getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> tokenService.parse(expired))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void invalidSignature_throwsInvalidToken() {
        String wrongSecret = "wrong-secret-min-256-bits-for-hs256-demo-key!!";
        String token = Jwt.claims()
                .subject(USER_ID.toString())
                .claim("role", "ESTUDANTE")
                .expiresIn(3600)
                .jws()
                .sign(new HmacKey(wrongSecret.getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> tokenService.parse(token))
                .isInstanceOf(InvalidTokenException.class);
    }
}
