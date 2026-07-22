package com.fiap.feedbacks.domain.exception;

import java.util.UUID;

public class InscricaoAulaObrigatoriaException extends DomainException {

    public InscricaoAulaObrigatoriaException(UUID aulaId) {
        super("Inscrição na aula obrigatória antes de avaliar: " + aulaId);
    }
}
