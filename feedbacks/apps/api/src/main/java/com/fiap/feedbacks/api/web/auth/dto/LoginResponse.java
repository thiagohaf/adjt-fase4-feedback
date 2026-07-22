package com.fiap.feedbacks.api.web.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
