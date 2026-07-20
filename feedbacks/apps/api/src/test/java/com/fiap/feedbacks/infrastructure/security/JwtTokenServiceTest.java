package com.fiap.feedbacks.infrastructure.security;

import com.fiap.feedbacks.application.auth.dto.AuthenticatedUser;
import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.domain.exception.InvalidTokenException;
import com.fiap.feedbacks.domain.exception.TokenExpiredException;
import com.fiap.feedbacks.support.JwtTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "test-secret-min-256-bits-for-hs256-demo-key";
    private static final UUID USER_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(new JwtProperties(SECRET, Duration.ofHours(24)));
    }

    @Test
    void shouldGenerateAndParseToken() {
        var user = new AuthenticatedUser(USER_ID, "estudante@demo.fiap", Papel.ESTUDANTE);

        var token = jwtTokenService.generate(user);
        var parsed = jwtTokenService.parse(token);

        assertThat(parsed.id()).isEqualTo(USER_ID);
        assertThat(parsed.papel()).isEqualTo(Papel.ESTUDANTE);
        assertThat(jwtTokenService.expiresInSeconds()).isEqualTo(86400L);
    }

    @Test
    void shouldRejectExpiredToken() {
        var expiredToken = JwtTestHelper.createExpiredToken(SECRET, USER_ID, "ESTUDANTE");

        assertThatThrownBy(() -> jwtTokenService.parse(expiredToken))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void shouldRejectInvalidSignature() {
        var invalidToken = JwtTestHelper.createTokenWithInvalidSignature(USER_ID, "ESTUDANTE");

        assertThatThrownBy(() -> jwtTokenService.parse(invalidToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> jwtTokenService.parse("token-invalido"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
