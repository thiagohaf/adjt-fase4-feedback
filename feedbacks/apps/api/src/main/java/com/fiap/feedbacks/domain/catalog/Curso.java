package com.fiap.feedbacks.domain.catalog;

import java.time.Instant;
import java.util.UUID;

public record Curso(
        UUID id,
        String nome,
        String descricao,
        Instant criadoEm
) {
}
