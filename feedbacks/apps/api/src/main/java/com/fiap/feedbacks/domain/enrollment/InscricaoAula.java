package com.fiap.feedbacks.domain.enrollment;

import java.time.Instant;
import java.util.UUID;

public record InscricaoAula(
        UUID id,
        UUID estudanteId,
        UUID aulaId,
        UUID cursoId,
        Instant criadoEm
) {
}
