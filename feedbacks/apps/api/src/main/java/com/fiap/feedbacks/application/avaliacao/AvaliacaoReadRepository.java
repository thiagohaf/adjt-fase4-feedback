package com.fiap.feedbacks.application.avaliacao;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AvaliacaoReadRepository {

    List<Map<String, Object>> findAll();

    List<Map<String, Object>> findByEstudanteId(UUID estudanteId);
}
