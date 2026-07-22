package com.fiap.feedbacks.application.enrollment.port;

import com.fiap.feedbacks.domain.enrollment.InscricaoAula;

import java.util.UUID;

public interface InscricaoAulaRepository {

    InscricaoAula save(InscricaoAula inscricao);

    boolean existsByEstudanteIdAndAulaId(UUID estudanteId, UUID aulaId);
}
