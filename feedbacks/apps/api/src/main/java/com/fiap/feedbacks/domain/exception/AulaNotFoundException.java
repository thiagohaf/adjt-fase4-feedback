package com.fiap.feedbacks.domain.exception;

import java.util.UUID;

public class AulaNotFoundException extends DomainException {

    public AulaNotFoundException(UUID id) {
        super("Aula not found: " + id);
    }
}
