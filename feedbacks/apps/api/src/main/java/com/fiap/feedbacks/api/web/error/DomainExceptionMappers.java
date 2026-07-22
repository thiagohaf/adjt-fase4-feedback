package com.fiap.feedbacks.api.web.error;

import com.fiap.feedbacks.domain.exception.ForbiddenAccessException;
import com.fiap.feedbacks.domain.exception.InvalidCredentialsException;
import com.fiap.feedbacks.domain.exception.InvalidTokenException;
import com.fiap.feedbacks.domain.exception.TokenExpiredException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

public final class DomainExceptionMappers {

    private DomainExceptionMappers() {
    }

    @Provider
    public static class InvalidCredentialsExceptionMapper
            implements ExceptionMapper<InvalidCredentialsException> {

        @Override
        public Response toResponse(InvalidCredentialsException exception) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiErrorFactory.of("AUTH_INVALID_CREDENTIALS", exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InvalidTokenExceptionMapper implements ExceptionMapper<InvalidTokenException> {

        @Override
        public Response toResponse(InvalidTokenException exception) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiErrorFactory.of("AUTH_INVALID_TOKEN", exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class TokenExpiredExceptionMapper implements ExceptionMapper<TokenExpiredException> {

        @Override
        public Response toResponse(TokenExpiredException exception) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ApiErrorFactory.of("AUTH_TOKEN_EXPIRED", exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class ForbiddenAccessExceptionMapper
            implements ExceptionMapper<ForbiddenAccessException> {

        @Override
        public Response toResponse(ForbiddenAccessException exception) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiErrorFactory.of("AUTH_FORBIDDEN", exception.getMessage()))
                    .build();
        }
    }
}
