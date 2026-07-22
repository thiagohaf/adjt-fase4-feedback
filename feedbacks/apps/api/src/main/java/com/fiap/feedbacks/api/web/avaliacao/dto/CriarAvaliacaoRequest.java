package com.fiap.feedbacks.api.web.avaliacao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CriarAvaliacaoRequest(
        @NotNull UUID aulaId,
        @NotBlank String descricao,
        @NotNull @Min(0) @Max(10) Integer nota
) {
}
