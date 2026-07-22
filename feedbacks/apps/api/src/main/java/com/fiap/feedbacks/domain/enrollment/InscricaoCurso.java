package com.fiap.feedbacks.domain.enrollment;

import java.time.Instant;
import java.util.UUID;

public record InscricaoCurso(
        UUID id,
        UUID estudanteId,
        UUID cursoId,
        Instant criadoEm
) {
}
