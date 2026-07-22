package com.fiap.feedbacks.api.web.avaliacao.dto;

import com.fiap.feedbacks.domain.avaliacao.Avaliacao;
import com.fiap.feedbacks.domain.avaliacao.Urgencia;

import java.time.Instant;
import java.util.UUID;

public record AvaliacaoResponse(
        UUID id,
        UUID estudanteId,
        UUID aulaId,
        UUID cursoId,
        String descricao,
        int nota,
        Urgencia urgencia,
        Instant ocorridoEm
) {

    public static AvaliacaoResponse from(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.id(),
                avaliacao.estudanteId(),
                avaliacao.aulaId(),
                avaliacao.cursoId(),
                avaliacao.descricao(),
                avaliacao.nota(),
                avaliacao.urgencia(),
                avaliacao.ocorridoEm()
        );
    }
}
