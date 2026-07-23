package com.fiap.feedbacks.lambda.report.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvaliacaoSnapshotTest {

    @Test
    void rejeitaUrgenciaVaziaOuNula() {
        assertThatThrownBy(() -> new AvaliacaoSnapshot((short) 1, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AvaliacaoSnapshot((short) 1, " ", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaOcorridoEmNulo() {
        assertThatThrownBy(() -> new AvaliacaoSnapshot((short) 1, "ALTA", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
