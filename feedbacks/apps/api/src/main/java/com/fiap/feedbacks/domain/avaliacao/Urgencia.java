package com.fiap.feedbacks.domain.avaliacao;

public enum Urgencia {
    ALTA,
    MEDIA,
    BAIXA;

    public static Urgencia fromNota(int nota) {
        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("nota deve estar entre 0 e 10");
        }
        if (nota <= 4) {
            return ALTA;
        }
        if (nota <= 7) {
            return MEDIA;
        }
        return BAIXA;
    }
}
