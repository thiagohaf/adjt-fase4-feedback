package com.fiap.feedbacks.domain.exception;

import java.util.UUID;

public class CursoNotFoundException extends DomainException {

    public CursoNotFoundException(UUID id) {
        super("Curso not found: " + id);
    }
}
