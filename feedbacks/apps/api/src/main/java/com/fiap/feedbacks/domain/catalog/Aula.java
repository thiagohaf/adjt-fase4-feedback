package com.fiap.feedbacks.domain.catalog;

import java.time.Instant;
import java.util.UUID;

public record Aula(
        UUID id,
        UUID cursoId,
        String nome,
        String descricao,
        Instant criadoEm
) {
}
