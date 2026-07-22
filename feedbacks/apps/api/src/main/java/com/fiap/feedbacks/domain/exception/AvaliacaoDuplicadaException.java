package com.fiap.feedbacks.domain.exception;

public class AvaliacaoDuplicadaException extends DomainException {

    public AvaliacaoDuplicadaException() {
        super("Já existe Avaliação para este Estudante e Aula");
    }
}
