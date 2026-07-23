package com.fiap.feedbacks.lambda.report.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Janela civil {@code America/Sao_Paulo} — intervalo half-open {@code [inicio, fim)}
 * filtrado por {@code ocorrido_em} (SPEC-11.2, SPEC-17.1; D3).
 */
public record ReportWindow(Periodo periodo, Instant inicio, Instant fim, LocalDate keyDate) {

    public static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");

    public ReportWindow {
        if (periodo == null || inicio == null || fim == null || keyDate == null) {
            throw new IllegalArgumentException("ReportWindow: campos obrigatórios");
        }
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("ReportWindow: fim deve ser após inicio");
        }
    }

    /**
     * @param triggerInstant instante do disparo (cron ou invoke manual — mesmo algoritmo, AD-10)
     */
    public static ReportWindow forTrigger(Periodo periodo, Instant triggerInstant) {
        if (periodo == null || triggerInstant == null) {
            throw new IllegalArgumentException("periodo e triggerInstant obrigatórios");
        }
        LocalDate dayD = triggerInstant.atZone(ZONE_SP).toLocalDate();
        return switch (periodo) {
            case DIARIO -> {
                LocalDate previous = dayD.minusDays(1);
                Instant inicio = previous.atStartOfDay(ZONE_SP).toInstant();
                Instant fim = dayD.atStartOfDay(ZONE_SP).toInstant();
                yield new ReportWindow(periodo, inicio, fim, previous);
            }
            case SEMANAL -> {
                // 7 dias civis imediatamente anteriores a D → [D-7, D)
                LocalDate start = dayD.minusDays(7);
                Instant inicio = start.atStartOfDay(ZONE_SP).toInstant();
                Instant fim = dayD.atStartOfDay(ZONE_SP).toInstant();
                LocalDate lastIncluded = dayD.minusDays(1);
                yield new ReportWindow(periodo, inicio, fim, lastIncluded);
            }
        };
    }
}
