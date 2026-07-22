package com.fiap.feedbacks.application.enrollment.port;

import com.fiap.feedbacks.domain.enrollment.InscricaoCurso;

import java.util.UUID;

public interface InscricaoCursoRepository {

    InscricaoCurso save(InscricaoCurso inscricao);

    boolean existsByEstudanteIdAndCursoId(UUID estudanteId, UUID cursoId);
}
