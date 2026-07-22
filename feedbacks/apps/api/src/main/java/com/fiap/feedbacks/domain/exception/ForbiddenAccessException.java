package com.fiap.feedbacks.domain.exception;

public class ForbiddenAccessException extends DomainException {

    public ForbiddenAccessException() {
        super("Access forbidden");
    }
}
