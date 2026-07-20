package com.fiap.feedbacks.support;

import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.infrastructure.persistence.AvaliacaoEntity;
import com.fiap.feedbacks.infrastructure.persistence.AvaliacaoJpaRepository;
import com.fiap.feedbacks.infrastructure.persistence.UsuarioEntity;
import com.fiap.feedbacks.infrastructure.persistence.UsuarioJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Configuration
@Profile("test")
public class TestDataInitializer {

    @Bean
    CommandLineRunner seedTestData(
            UsuarioJpaRepository usuarioJpaRepository,
            AvaliacaoJpaRepository avaliacaoJpaRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (usuarioJpaRepository.count() > 0) {
                return;
            }

            var estudanteA = new UsuarioEntity(
                    UUID.fromString("a1111111-1111-4111-8111-111111111111"),
                    "estudante@demo.fiap",
                    passwordEncoder.encode("senha123"),
                    Papel.ESTUDANTE,
                    OffsetDateTime.now()
            );
            var admin = new UsuarioEntity(
                    UUID.fromString("b2222222-2222-4222-8222-222222222222"),
                    "admin@demo.fiap",
                    passwordEncoder.encode("admin123"),
                    Papel.ADMINISTRADOR,
                    OffsetDateTime.now()
            );
            var estudanteB = new UsuarioEntity(
                    UUID.fromString("c3333333-3333-4333-8333-333333333333"),
                    "estudante2@demo.fiap",
                    passwordEncoder.encode("senha123"),
                    Papel.ESTUDANTE,
                    OffsetDateTime.now()
            );
            usuarioJpaRepository.saveAll(java.util.List.of(estudanteA, admin, estudanteB));

            avaliacaoJpaRepository.saveAll(java.util.List.of(
                    new AvaliacaoEntity(
                            UUID.fromString("d4444444-4444-4444-8444-444444444444"),
                            estudanteA.getId(),
                            "Feedback do Estudante A"
                    ),
                    new AvaliacaoEntity(
                            UUID.fromString("e5555555-5555-4555-8555-555555555555"),
                            estudanteB.getId(),
                            "Feedback do Estudante B"
                    )
            ));
        };
    }
}
