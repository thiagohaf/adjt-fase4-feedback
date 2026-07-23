package com.fiap.feedbacks.lambda.report.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoTest {

    @ParameterizedTest
    @CsvSource({"diario,DIARIO", "DIARIO,DIARIO", " Semanal ,SEMANAL", "semanal,SEMANAL"})
    void fromWireAceitaValoresValidos(String raw, Periodo expected) {
        assertThat(Periodo.fromWire(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "mensal", "daily"})
    void fromWireRejeitaInvalidos(String raw) {
        assertThatThrownBy(() -> Periodo.fromWire(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodo");
    }

    @Test
    void wireValue() {
        assertThat(Periodo.DIARIO.wireValue()).isEqualTo("diario");
        assertThat(Periodo.SEMANAL.wireValue()).isEqualTo("semanal");
    }
}
