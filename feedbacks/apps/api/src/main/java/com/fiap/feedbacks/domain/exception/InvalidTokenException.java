package com.fiap.feedbacks.domain.exception;

public class InvalidTokenException extends DomainException {

    public InvalidTokenException() {
        super("Invalid token");
    }

    public InvalidTokenException(String message) {
        super(message);
    }
}
