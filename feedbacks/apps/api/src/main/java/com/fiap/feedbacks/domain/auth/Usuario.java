package com.fiap.feedbacks.domain.auth;

import java.util.UUID;

public record Usuario(
        UUID id,
        String email,
        String senhaHash,
        Papel papel
) {
}
