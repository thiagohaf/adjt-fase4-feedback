package com.fiap.feedbacks.api.web.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

public final class ValidationExceptionMapper {

    private ValidationExceptionMapper() {
    }

    @Provider
    public static class ConstraintViolationExceptionMapper
            implements ExceptionMapper<ConstraintViolationException> {

        @Override
        public Response toResponse(ConstraintViolationException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiErrorFactory.of("VALIDATION_ERROR", "Invalid request payload"))
                    .build();
        }
    }

    @Provider
    public static class JakartaValidationExceptionMapper implements ExceptionMapper<ValidationException> {

        @Override
        public Response toResponse(ValidationException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ApiErrorFactory.of("VALIDATION_ERROR", "Invalid request payload"))
                    .build();
        }
    }
}
