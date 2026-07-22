package com.fiap.feedbacks.api.web.error;

public record ApiErrorResponse(
        String code,
        String message,
        String traceId
) {
}
