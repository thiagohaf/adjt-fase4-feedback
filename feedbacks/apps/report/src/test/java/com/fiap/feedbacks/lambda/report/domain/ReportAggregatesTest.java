package com.fiap.feedbacks.lambda.report.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SPEC-11.1 / SPEC-17.1 / SPEC-12.5 — agregados e período vazio.
 */
class ReportAggregatesTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Test
    void diarioAgregaMediaTotalEUrgencia() {
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        ReportWindow window = ReportWindow.forTrigger(Periodo.DIARIO, trigger);
        Instant mid = LocalDate.of(2026, 7, 21).atTime(12, 0).atZone(SP).toInstant();

        ReportAggregates agg = ReportAggregates.from(
                window,
                List.of(
                        new AvaliacaoSnapshot((short) 8, "ALTA", mid),
                        new AvaliacaoSnapshot((short) 6, "MEDIA", mid),
                        new AvaliacaoSnapshot((short) 4, "BAIXA", mid)));

        assertThat(agg.total()).isEqualTo(3);
        assertThat(agg.mediaNota()).isEqualTo(6.0);
        assertThat(agg.qtyPorUrgencia())
                .containsEntry("ALTA", 1L)
                .containsEntry("MEDIA", 1L)
                .containsEntry("BAIXA", 1L);
        assertThat(agg.qtyPorDia()).containsEntry(LocalDate.of(2026, 7, 21), 3L);
    }

    @Test
    void semanalAgregaPorDiaCivilSp() {
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        ReportWindow window = ReportWindow.forTrigger(Periodo.SEMANAL, trigger);

        Instant d15 = LocalDate.of(2026, 7, 15).atTime(10, 0).atZone(SP).toInstant();
        Instant d16 = LocalDate.of(2026, 7, 16).atTime(10, 0).atZone(SP).toInstant();
        Instant outside = LocalDate.of(2026, 7, 14).atTime(23, 59).atZone(SP).toInstant();

        // outside não deve ser passado pelo read model; aqui só agregamos o que entra
        ReportAggregates agg = ReportAggregates.from(
                window,
                List.of(
                        new AvaliacaoSnapshot((short) 10, "ALTA", d15),
                        new AvaliacaoSnapshot((short) 5, "ALTA", d15),
                        new AvaliacaoSnapshot((short) 7, "MEDIA", d16)));

        assertThat(agg.total()).isEqualTo(3);
        assertThat(agg.mediaNota()).isEqualTo(7.333333333333333, org.assertj.core.data.Offset.offset(0.001));
        assertThat(agg.qtyPorDia())
                .containsEntry(LocalDate.of(2026, 7, 15), 2L)
                .containsEntry(LocalDate.of(2026, 7, 16), 1L)
                .doesNotContainKey(LocalDate.of(2026, 7, 14));
        assertThat(agg.qtyPorUrgencia()).containsEntry("ALTA", 2L).containsEntry("MEDIA", 1L);
        assertThat(outside).isBefore(window.inicio());
    }

    @Test
    void periodoVazioEntregaZeros() {
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        ReportWindow window = ReportWindow.forTrigger(Periodo.DIARIO, trigger);

        ReportAggregates agg = ReportAggregates.from(window, List.of());

        assertThat(agg.isEmpty()).isTrue();
        assertThat(agg.total()).isZero();
        assertThat(agg.mediaNota()).isZero();
        assertThat(agg.qtyPorDia()).isEmpty();
        assertThat(agg.qtyPorUrgencia())
                .containsEntry("ALTA", 0L)
                .containsEntry("MEDIA", 0L)
                .containsEntry("BAIXA", 0L);
    }

    @Test
    void fromRejeitaWindowNulaEAceitaRowsNulasComoVazio() {
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        ReportWindow window = ReportWindow.forTrigger(Periodo.DIARIO, trigger);
        assertThatThrownBy(() -> ReportAggregates.from(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ReportAggregates.from(window, null).total()).isZero();
    }

    @Test
    void compactConstructorNormalizaMapasNulosEIsEmptyFalso() {
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        ReportWindow window = ReportWindow.forTrigger(Periodo.DIARIO, trigger);
        ReportAggregates withNullMaps = new ReportAggregates(Periodo.DIARIO, window, 0.0, 0, null, null);
        assertThat(withNullMaps.qtyPorDia()).isEmpty();
        assertThat(withNullMaps.qtyPorUrgencia()).isEmpty();
        assertThat(withNullMaps.isEmpty()).isTrue();

        ReportAggregates nonEmpty = new ReportAggregates(Periodo.DIARIO, window, 8.0, 2, Map.of(), Map.of());
        assertThat(nonEmpty.isEmpty()).isFalse();
    }
}
