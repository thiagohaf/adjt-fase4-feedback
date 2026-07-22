package com.fiap.feedbacks.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordEncoderTest {

    @Test
    void encodeAndMatches() {
        var encoder = new BcryptPasswordEncoder();
        String hash = encoder.encode("senha123");
        assertThat(hash).startsWith("$2");
        assertThat(encoder.matches("senha123", hash)).isTrue();
        assertThat(encoder.matches("errada", hash)).isFalse();
    }
}
