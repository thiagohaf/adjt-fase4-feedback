package com.fiap.feedbacks.domain.exception;

public class InscricaoDuplicadaException extends DomainException {

    public InscricaoDuplicadaException() {
        super("Inscrição duplicada");
    }
}
