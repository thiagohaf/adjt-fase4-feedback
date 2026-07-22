package com.fiap.feedbacks.api.web.avaliacao.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CriarAvaliacaoStubRequest(
        @NotNull UUID aulaId,
        String descricao
) {
}
