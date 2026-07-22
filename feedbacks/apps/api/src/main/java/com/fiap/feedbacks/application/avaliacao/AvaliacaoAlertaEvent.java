package com.fiap.feedbacks.application.avaliacao;

import com.fiap.feedbacks.domain.avaliacao.Avaliacao;
import com.fiap.feedbacks.domain.avaliacao.Urgencia;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload AD-5 para alerta de Urgência ALTA.
 */
public record AvaliacaoAlertaEvent(
        UUID avaliacaoId,
        String descricao,
        Urgencia urgencia,
        Instant ocorridoEm,
        UUID aulaId,
        UUID cursoId
) {

    public static AvaliacaoAlertaEvent from(Avaliacao avaliacao) {
        return new AvaliacaoAlertaEvent(
                avaliacao.id(),
                avaliacao.descricao(),
                avaliacao.urgencia(),
                avaliacao.ocorridoEm(),
                avaliacao.aulaId(),
                avaliacao.cursoId()
        );
    }
}
