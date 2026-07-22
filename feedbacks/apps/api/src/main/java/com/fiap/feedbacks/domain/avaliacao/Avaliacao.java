package com.fiap.feedbacks.domain.avaliacao;

import java.time.Instant;
import java.util.UUID;

public record Avaliacao(
        UUID id,
        UUID estudanteId,
        UUID aulaId,
        UUID cursoId,
        String descricao,
        int nota,
        Urgencia urgencia,
        Instant ocorridoEm
) {

    public static Avaliacao criar(
            UUID estudanteId,
            UUID aulaId,
            UUID cursoId,
            String descricao,
            int nota
    ) {
        if (estudanteId == null || aulaId == null || cursoId == null) {
            throw new IllegalArgumentException("estudanteId, aulaId e cursoId são obrigatórios");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("descricao é obrigatória");
        }
        if (descricao.length() > 500) {
            throw new IllegalArgumentException("descricao deve ter no máximo 500 caracteres");
        }
        Urgencia urgencia = Urgencia.fromNota(nota);
        return new Avaliacao(
                UUID.randomUUID(),
                estudanteId,
                aulaId,
                cursoId,
                descricao.trim(),
                nota,
                urgencia,
                Instant.now()
        );
    }
}
