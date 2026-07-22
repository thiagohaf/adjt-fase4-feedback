package com.fiap.feedbacks.api.web.catalog.dto;

import java.util.UUID;

public record CursoResponse(
        UUID id,
        String nome,
        String descricao
) {
}
