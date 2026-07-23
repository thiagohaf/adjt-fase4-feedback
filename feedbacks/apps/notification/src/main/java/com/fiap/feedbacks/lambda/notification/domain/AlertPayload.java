package com.fiap.feedbacks.lambda.notification.domain;

import java.util.Objects;

/**
 * Contrato AD-5 espelhado (campos canônicos do evento de alerta ALTA).
 */
public record AlertPayload(
        String avaliacaoId,
        String descricao,
        String urgencia,
        String ocorridoEm,
        String aulaId,
        String cursoId
) {

    public AlertPayload {
        Objects.requireNonNull(avaliacaoId, "avaliacaoId");
        Objects.requireNonNull(descricao, "descricao");
        Objects.requireNonNull(urgencia, "urgencia");
        Objects.requireNonNull(ocorridoEm, "ocorridoEm");
        Objects.requireNonNull(aulaId, "aulaId");
        Objects.requireNonNull(cursoId, "cursoId");
        if (avaliacaoId.isBlank() || descricao.isBlank() || urgencia.isBlank()
                || ocorridoEm.isBlank() || aulaId.isBlank() || cursoId.isBlank()) {
            throw new IllegalArgumentException("Campos AD-5 não podem ser vazios");
        }
    }
}
