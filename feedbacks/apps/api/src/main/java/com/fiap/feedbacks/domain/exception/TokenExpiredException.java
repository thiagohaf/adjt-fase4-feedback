package com.fiap.feedbacks.domain.exception;

public class TokenExpiredException extends DomainException {

    public TokenExpiredException() {
        super("Token expired");
    }
}
