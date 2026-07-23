package com.fiap.feedbacks.lambda.report.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SPEC-11.2 / SPEC-17.1 — fronteiras civis America/Sao_Paulo (meia-noite).
 */
class ReportWindowTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Test
    void diarioUsaDiaCivilAnteriorIncluindoFronteiraMeiaNoite() {
        // Disparo em 2026-07-22 00:30 SP → janela = dia 21 inteiro
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 0, 30, 0, 0, SP).toInstant();
        ReportWindow w = ReportWindow.forTrigger(Periodo.DIARIO, trigger);

        Instant expectedInicio = LocalDate.of(2026, 7, 21).atStartOfDay(SP).toInstant();
        Instant expectedFim = LocalDate.of(2026, 7, 22).atStartOfDay(SP).toInstant();

        assertThat(w.inicio()).isEqualTo(expectedInicio);
        assertThat(w.fim()).isEqualTo(expectedFim);
        assertThat(w.keyDate()).isEqualTo(LocalDate.of(2026, 7, 21));
        assertThat(w.periodo()).isEqualTo(Periodo.DIARIO);
    }

    @Test
    void diarioExcluiInstanteNaMeiaNoiteDoDiaD() {
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        ReportWindow w = ReportWindow.forTrigger(Periodo.DIARIO, trigger);
        Instant midnightD = LocalDate.of(2026, 7, 22).atStartOfDay(SP).toInstant();
        Instant justBefore = midnightD.minusNanos(1);

        assertThat(justBefore).isBefore(w.fim());
        assertThat(midnightD).isEqualTo(w.fim());
        assertThat(!justBefore.isBefore(w.inicio()) && justBefore.isBefore(w.fim())).isTrue();
        assertThat(midnightD.isBefore(w.fim())).isFalse();
    }

    @Test
    void semanalUsaSeteDiasCivisAnterioresANAoDiaD() {
        // D = 2026-07-22 (quarta) → [2026-07-15, 2026-07-22)
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        ReportWindow w = ReportWindow.forTrigger(Periodo.SEMANAL, trigger);

        assertThat(w.inicio()).isEqualTo(LocalDate.of(2026, 7, 15).atStartOfDay(SP).toInstant());
        assertThat(w.fim()).isEqualTo(LocalDate.of(2026, 7, 22).atStartOfDay(SP).toInstant());
        assertThat(w.keyDate()).isEqualTo(LocalDate.of(2026, 7, 21));
    }

    @Test
    void invokeManualUsaMesmoAlgoritmoDoCron() {
        Instant t = ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, SP).toInstant();
        assertThat(ReportWindow.forTrigger(Periodo.DIARIO, t))
                .isEqualTo(ReportWindow.forTrigger(Periodo.DIARIO, t));
        assertThat(ReportWindow.forTrigger(Periodo.SEMANAL, t).inicio())
                .isEqualTo(LocalDate.of(2026, 3, 3).atStartOfDay(SP).toInstant());
    }

    @Test
    void rejeitaArgsNulos() {
        assertThatThrownBy(() -> ReportWindow.forTrigger(null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReportWindow.forTrigger(Periodo.DIARIO, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compactConstructorValidaCampos() {
        Instant a = Instant.parse("2026-07-21T03:00:00Z");
        Instant b = Instant.parse("2026-07-22T03:00:00Z");
        LocalDate d = LocalDate.of(2026, 7, 21);
        assertThatThrownBy(() -> new ReportWindow(null, a, b, d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReportWindow(Periodo.DIARIO, null, b, d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReportWindow(Periodo.DIARIO, a, null, d))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReportWindow(Periodo.DIARIO, a, b, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReportWindow(Periodo.DIARIO, a, a, d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fim");
    }
}
