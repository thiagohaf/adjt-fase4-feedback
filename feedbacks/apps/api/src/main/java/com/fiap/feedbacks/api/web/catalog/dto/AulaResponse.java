package com.fiap.feedbacks.api.web.catalog.dto;

import java.util.UUID;

public record AulaResponse(
        UUID id,
        UUID cursoId,
        String nome,
        String descricao
) {
}
