package com.fiap.feedbacks.lambda.report.domain;

import java.util.Locale;

/**
 * Modo do relatório — uma Lambda, parâmetro {@code periodo} (AD-6 / D2).
 */
public enum Periodo {
    DIARIO("diario"),
    SEMANAL("semanal");

    private final String wireValue;

    Periodo(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    /**
     * Parse do payload JSON; valores inválidos falham de forma explícita (SPEC-12 / task 4.2).
     */
    public static Periodo fromWire(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("periodo obrigatório: diario|semanal");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (Periodo p : values()) {
            if (p.wireValue.equals(normalized)) {
                return p;
            }
        }
        throw new IllegalArgumentException("periodo inválido: '" + raw + "' (esperado: diario|semanal)");
    }
}
