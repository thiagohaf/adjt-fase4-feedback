package com.fiap.feedbacks.application.auth.dto;

import com.fiap.feedbacks.domain.auth.Papel;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        Papel papel
) {
}
