package com.fiap.feedbacks.lambda.notification.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertPayloadTest {

    @Test
    void criaPayloadValido() {
        var payload = new AlertPayload("id", "desc", "ALTA", "2026-01-01T00:00:00Z", "a", "c");
        assertThat(payload.urgencia()).isEqualTo("ALTA");
    }

    @ParameterizedTest
    @CsvSource({
            "' ', desc, ALTA, t, a, c",
            "id, ' ', ALTA, t, a, c",
            "id, desc, ' ', t, a, c",
            "id, desc, ALTA, ' ', a, c",
            "id, desc, ALTA, t, ' ', c",
            "id, desc, ALTA, t, a, ' '"
    })
    void rejeitaCampoEmBranco(String avaliacaoId, String descricao, String urgencia,
                              String ocorridoEm, String aulaId, String cursoId) {
        assertThatThrownBy(() -> new AlertPayload(avaliacaoId, descricao, urgencia, ocorridoEm, aulaId, cursoId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "avaliacaoId",
            "descricao",
            "urgencia",
            "ocorridoEm",
            "aulaId",
            "cursoId"
    })
    void rejeitaNullPorCampo(String campoNulo) {
        String avaliacaoId = "avaliacaoId".equals(campoNulo) ? null : "id";
        String descricao = "descricao".equals(campoNulo) ? null : "d";
        String urgencia = "urgencia".equals(campoNulo) ? null : "ALTA";
        String ocorridoEm = "ocorridoEm".equals(campoNulo) ? null : "t";
        String aulaId = "aulaId".equals(campoNulo) ? null : "a";
        String cursoId = "cursoId".equals(campoNulo) ? null : "c";
        assertThatThrownBy(() -> new AlertPayload(avaliacaoId, descricao, urgencia, ocorridoEm, aulaId, cursoId))
                .isInstanceOf(NullPointerException.class);
    }
}
