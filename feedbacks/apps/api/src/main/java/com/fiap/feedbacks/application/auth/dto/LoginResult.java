package com.fiap.feedbacks.application.auth.dto;

public record LoginResult(String accessToken, long expiresInSeconds) {
}
