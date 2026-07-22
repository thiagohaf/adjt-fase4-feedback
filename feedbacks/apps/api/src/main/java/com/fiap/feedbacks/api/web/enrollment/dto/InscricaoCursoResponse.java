package com.fiap.feedbacks.api.web.enrollment.dto;

import java.util.UUID;

public record InscricaoCursoResponse(
        UUID id,
        UUID cursoId,
        UUID estudanteId
) {
}
