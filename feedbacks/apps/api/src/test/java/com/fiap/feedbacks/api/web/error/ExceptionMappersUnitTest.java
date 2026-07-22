package com.fiap.feedbacks.api.web.error;

import com.fiap.feedbacks.domain.exception.ForbiddenAccessException;
import com.fiap.feedbacks.domain.exception.InvalidCredentialsException;
import com.fiap.feedbacks.domain.exception.InvalidTokenException;
import com.fiap.feedbacks.domain.exception.TokenExpiredException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionMappersUnitTest {

    @Test
    void domainMappers_returnExpectedCodes() {
        var credentials = new DomainExceptionMappers.InvalidCredentialsExceptionMapper()
                .toResponse(new InvalidCredentialsException());
        assertThat(credentials.getStatus()).isEqualTo(401);
        assertThat(((ApiErrorResponse) credentials.getEntity()).code()).isEqualTo("AUTH_INVALID_CREDENTIALS");

        var invalid = new DomainExceptionMappers.InvalidTokenExceptionMapper()
                .toResponse(new InvalidTokenException());
        assertThat(invalid.getStatus()).isEqualTo(401);
        assertThat(((ApiErrorResponse) invalid.getEntity()).code()).isEqualTo("AUTH_INVALID_TOKEN");

        var expired = new DomainExceptionMappers.TokenExpiredExceptionMapper()
                .toResponse(new TokenExpiredException());
        assertThat(expired.getStatus()).isEqualTo(401);
        assertThat(((ApiErrorResponse) expired.getEntity()).code()).isEqualTo("AUTH_TOKEN_EXPIRED");

        var forbidden = new DomainExceptionMappers.ForbiddenAccessExceptionMapper()
                .toResponse(new ForbiddenAccessException());
        assertThat(forbidden.getStatus()).isEqualTo(403);
        assertThat(((ApiErrorResponse) forbidden.getEntity()).code()).isEqualTo("AUTH_FORBIDDEN");
    }

    @Test
    void authAndValidationMappers_returnExpectedCodes() {
        var missing = new AuthExceptionMappers.UnauthorizedExceptionMapper()
                .toResponse(new UnauthorizedException());
        assertThat(((ApiErrorResponse) missing.getEntity()).code()).isEqualTo("AUTH_MISSING_TOKEN");

        var authFailed = new AuthExceptionMappers.AuthenticationFailedExceptionMapper();
        var invalid = authFailed.toResponse(new AuthenticationFailedException("bad token"));
        assertThat(((ApiErrorResponse) invalid.getEntity()).code()).isEqualTo("AUTH_INVALID_TOKEN");

        var expiredCause = new AuthenticationFailedException("jwt expired", new RuntimeException("The JWT is no longer valid"));
        var expired = authFailed.toResponse(expiredCause);
        assertThat(((ApiErrorResponse) expired.getEntity()).code()).isEqualTo("AUTH_TOKEN_EXPIRED");

        var forbidden = new AuthExceptionMappers.ForbiddenExceptionMapper()
                .toResponse(new ForbiddenException());
        assertThat(((ApiErrorResponse) forbidden.getEntity()).code()).isEqualTo("AUTH_FORBIDDEN");

        var constraint = new ValidationExceptionMapper.ConstraintViolationExceptionMapper()
                .toResponse(new ConstraintViolationException("invalid", Set.of()));
        assertThat(((ApiErrorResponse) constraint.getEntity()).code()).isEqualTo("VALIDATION_ERROR");

        var validation = new ValidationExceptionMapper.JakartaValidationExceptionMapper()
                .toResponse(new ValidationException("invalid"));
        assertThat(((ApiErrorResponse) validation.getEntity()).code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void payloadExpInPast_detectsExpiredJwt() {
        // header.payload.sig — exp in the past
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"x\",\"exp\":1}".getBytes());
        String token = "eyJhbGciOiJIUzI1NiJ9." + payload + ".sig";
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast(token)).isTrue();
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast("not-a-jwt")).isFalse();
        assertThat(AuthExceptionMappers.AuthenticationFailedExceptionMapper.payloadExpInPast(null)).isFalse();
    }

    @Test
    void invalidTokenException_alternateConstructor() {
        assertThat(new InvalidTokenException("custom").getMessage()).isEqualTo("custom");
        assertThat(new ForbiddenAccessException().getMessage()).isEqualTo("Access forbidden");
    }
}
