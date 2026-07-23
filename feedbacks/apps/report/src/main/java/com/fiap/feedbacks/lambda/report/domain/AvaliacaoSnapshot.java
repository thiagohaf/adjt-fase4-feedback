package com.fiap.feedbacks.lambda.report.domain;

import java.time.Instant;

/**
 * Projeção read-only para agregação (AD-16) — sem mutação de domínio.
 */
public record AvaliacaoSnapshot(short nota, String urgencia, Instant ocorridoEm) {

    public AvaliacaoSnapshot {
        if (urgencia == null || urgencia.isBlank()) {
            throw new IllegalArgumentException("urgencia obrigatória");
        }
        if (ocorridoEm == null) {
            throw new IllegalArgumentException("ocorridoEm obrigatório");
        }
    }
}
