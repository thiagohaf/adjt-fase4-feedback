package com.fiap.feedbacks.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.feedbacks.api.web.error.ApiErrorResponse;
import com.fiap.feedbacks.domain.exception.InvalidTokenException;
import com.fiap.feedbacks.domain.exception.TokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        var jwtException = request.getAttribute("jwtAuthException");
        String code;
        String message;

        if (jwtException instanceof TokenExpiredException) {
            code = "AUTH_TOKEN_EXPIRED";
            message = "Token expired";
        } else if (jwtException instanceof InvalidTokenException) {
            code = "AUTH_INVALID_TOKEN";
            message = "Invalid token";
        } else {
            code = "AUTH_MISSING_TOKEN";
            message = "Missing or invalid authentication token";
        }

        writeError(response, code, message);
    }

    private void writeError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var traceId = MDC.get("traceId");
        OBJECT_MAPPER.writeValue(response.getOutputStream(), new ApiErrorResponse(code, message, traceId));
    }
}
