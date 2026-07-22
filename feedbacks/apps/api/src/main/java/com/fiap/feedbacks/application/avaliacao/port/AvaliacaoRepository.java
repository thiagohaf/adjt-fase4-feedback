package com.fiap.feedbacks.application.avaliacao.port;

import com.fiap.feedbacks.domain.avaliacao.Avaliacao;

import java.util.List;
import java.util.UUID;

public interface AvaliacaoRepository {

    Avaliacao save(Avaliacao avaliacao);

    boolean existsByEstudanteIdAndAulaId(UUID estudanteId, UUID aulaId);

    List<Avaliacao> findAll();

    List<Avaliacao> findByEstudanteId(UUID estudanteId);
}
