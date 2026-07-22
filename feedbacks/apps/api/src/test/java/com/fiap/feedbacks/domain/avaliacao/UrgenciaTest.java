package com.fiap.feedbacks.domain.avaliacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrgenciaTest {

    @ParameterizedTest(name = "nota {0} → {1}")
    @CsvSource({
            "0, ALTA",
            "4, ALTA",
            "5, MEDIA",
            "7, MEDIA",
            "8, BAIXA",
            "10, BAIXA"
    })
    @DisplayName("Fronteiras 4/5/7/8 e extremos")
    void classificaNota(int nota, Urgencia esperada) {
        assertThat(Urgencia.fromNota(nota)).isEqualTo(esperada);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    void notaInvalidaRejeita(int nota) {
        assertThatThrownBy(() -> Urgencia.fromNota(nota))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void avaliacaoCriarDerivaUrgenciaEOcorridoEm() {
        UUID estudanteId = UUID.randomUUID();
        UUID aulaId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();

        Avaliacao avaliacao = Avaliacao.criar(estudanteId, aulaId, cursoId, "Aula rápida", 3);

        assertThat(avaliacao.id()).isNotNull();
        assertThat(avaliacao.estudanteId()).isEqualTo(estudanteId);
        assertThat(avaliacao.aulaId()).isEqualTo(aulaId);
        assertThat(avaliacao.cursoId()).isEqualTo(cursoId);
        assertThat(avaliacao.descricao()).isEqualTo("Aula rápida");
        assertThat(avaliacao.nota()).isEqualTo(3);
        assertThat(avaliacao.urgencia()).isEqualTo(Urgencia.ALTA);
        assertThat(avaliacao.ocorridoEm()).isNotNull();
    }

    @Test
    void avaliacaoCriarRejeitaDescricaoBlank() {
        assertThatThrownBy(() -> Avaliacao.criar(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "  ", 5
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void avaliacaoCriarRejeitaDescricaoLonga() {
        String longa = "x".repeat(501);
        assertThatThrownBy(() -> Avaliacao.criar(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), longa, 5
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void avaliacaoCriarRejeitaIdsNulos() {
        assertThatThrownBy(() -> Avaliacao.criar(null, UUID.randomUUID(), UUID.randomUUID(), "ok", 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
