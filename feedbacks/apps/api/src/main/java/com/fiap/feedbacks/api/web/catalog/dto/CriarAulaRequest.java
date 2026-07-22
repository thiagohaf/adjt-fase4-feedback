package com.fiap.feedbacks.api.web.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarAulaRequest(
        @NotBlank String nome,
        String descricao
) {
}
