package com.fiap.feedbacks.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.feedbacks.api.web.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var traceId = MDC.get("traceId");
        OBJECT_MAPPER.writeValue(
                response.getOutputStream(),
                new ApiErrorResponse("AUTH_FORBIDDEN", "Access forbidden", traceId)
        );
    }
}
