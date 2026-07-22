package com.fiap.feedbacks.infrastructure.security;

import com.fiap.feedbacks.application.auth.port.PasswordEncoder;
import com.fiap.feedbacks.infrastructure.persistence.UsuarioJpaRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PasswordHashNfrTest {

    @Inject
    UsuarioJpaRepository usuarioJpaRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("SPEC-NFR-3 — senha armazenada com hash BCrypt")
    void specNfr3_senhaComHash() {
        var usuario = usuarioJpaRepository.findByEmail("estudante@demo.fiap").orElseThrow();

        assertThat(usuario.getSenhaHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("senha123", usuario.getSenhaHash())).isTrue();
        assertThat(usuario.getSenhaHash()).isNotEqualTo("senha123");
    }
}
