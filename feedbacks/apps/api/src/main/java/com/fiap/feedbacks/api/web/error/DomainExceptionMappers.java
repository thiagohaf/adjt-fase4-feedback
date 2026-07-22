package com.fiap.feedbacks.api.web.error;

import com.fiap.feedbacks.domain.exception.AulaNotFoundException;
import com.fiap.feedbacks.domain.exception.AvaliacaoDuplicadaException;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
import com.fiap.feedbacks.domain.exception.ForbiddenAccessException;
import com.fiap.feedbacks.domain.exception.InscricaoAulaObrigatoriaException;
import com.fiap.feedbacks.domain.exception.InscricaoCursoObrigatoriaException;
import com.fiap.feedbacks.domain.exception.InscricaoDuplicadaException;
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

    @Provider
    public static class CursoNotFoundExceptionMapper
            implements ExceptionMapper<CursoNotFoundException> {

        @Override
        public Response toResponse(CursoNotFoundException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiErrorFactory.of("CURSO_NOT_FOUND", exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class AulaNotFoundExceptionMapper
            implements ExceptionMapper<AulaNotFoundException> {

        @Override
        public Response toResponse(AulaNotFoundException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ApiErrorFactory.of("AULA_NOT_FOUND", exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InscricaoDuplicadaExceptionMapper
            implements ExceptionMapper<InscricaoDuplicadaException> {

        @Override
        public Response toResponse(InscricaoDuplicadaException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ApiErrorFactory.of("INSCRICAO_DUPLICADA", exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class AvaliacaoDuplicadaExceptionMapper
            implements ExceptionMapper<AvaliacaoDuplicadaException> {

        @Override
        public Response toResponse(AvaliacaoDuplicadaException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ApiErrorFactory.of("AVALIACAO_DUPLICADA", exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InscricaoCursoObrigatoriaExceptionMapper
            implements ExceptionMapper<InscricaoCursoObrigatoriaException> {

        @Override
        public Response toResponse(InscricaoCursoObrigatoriaException exception) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(ApiErrorFactory.of("INSCRICAO_CURSO_OBRIGATORIA", exception.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class InscricaoAulaObrigatoriaExceptionMapper
            implements ExceptionMapper<InscricaoAulaObrigatoriaException> {

        @Override
        public Response toResponse(InscricaoAulaObrigatoriaException exception) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(ApiErrorFactory.of("INSCRICAO_AULA_OBRIGATORIA", exception.getMessage()))
                    .build();
        }
    }
}
