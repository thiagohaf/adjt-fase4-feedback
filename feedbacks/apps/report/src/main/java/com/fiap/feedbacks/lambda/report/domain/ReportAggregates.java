package com.fiap.feedbacks.lambda.report.domain;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Agregados AD-16 / SPEC-11.1 / SPEC-17.1. Período vazio → zeros / mapas vazios (SPEC-12.5).
 */
public record ReportAggregates(
        Periodo periodo,
        ReportWindow window,
        double mediaNota,
        long total,
        Map<LocalDate, Long> qtyPorDia,
        Map<String, Long> qtyPorUrgencia) {

    public static final List<String> URGENCIAS = List.of("ALTA", "MEDIA", "BAIXA");

    public ReportAggregates {
        qtyPorDia = qtyPorDia == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(qtyPorDia));
        qtyPorUrgencia = qtyPorUrgencia == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(qtyPorUrgencia));
    }

    public static ReportAggregates from(ReportWindow window, List<AvaliacaoSnapshot> rows) {
        if (window == null) {
            throw new IllegalArgumentException("window obrigatória");
        }
        List<AvaliacaoSnapshot> safe = rows == null ? List.of() : rows;
        long total = safe.size();
        double media = 0.0;
        if (total > 0) {
            long sum = 0;
            for (AvaliacaoSnapshot row : safe) {
                sum += row.nota();
            }
            media = (double) sum / total;
        }

        Map<String, Long> byUrgencia = new LinkedHashMap<>();
        for (String u : URGENCIAS) {
            byUrgencia.put(u, 0L);
        }
        Map<LocalDate, Long> byDia = new TreeMap<>();

        for (AvaliacaoSnapshot row : safe) {
            String urg = row.urgencia().trim().toUpperCase();
            byUrgencia.merge(urg, 1L, Long::sum);
            LocalDate dia = row.ocorridoEm().atZone(ReportWindow.ZONE_SP).toLocalDate();
            byDia.merge(dia, 1L, Long::sum);
        }

        // Diário: qty total já em total; qty/dia permanece para consistência do PDF
        return new ReportAggregates(window.periodo(), window, media, total, byDia, byUrgencia);
    }

    public boolean isEmpty() {
        return total == 0;
    }
}
