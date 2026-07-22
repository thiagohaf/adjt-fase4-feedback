package com.fiap.feedbacks.domain.exception;

import java.util.UUID;

public class InscricaoCursoObrigatoriaException extends DomainException {

    public InscricaoCursoObrigatoriaException(UUID cursoId) {
        super("Inscrição no curso obrigatória antes de inscrever na aula: " + cursoId);
    }
}
