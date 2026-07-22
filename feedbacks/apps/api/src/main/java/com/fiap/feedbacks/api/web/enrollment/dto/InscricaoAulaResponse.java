package com.fiap.feedbacks.api.web.enrollment.dto;

import java.util.UUID;

public record InscricaoAulaResponse(
        UUID id,
        UUID cursoId,
        UUID aulaId,
        UUID estudanteId
) {
}
