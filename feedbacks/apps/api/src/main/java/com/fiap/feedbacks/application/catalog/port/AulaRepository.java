package com.fiap.feedbacks.application.catalog.port;

import com.fiap.feedbacks.domain.catalog.Aula;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AulaRepository {

    Aula save(Aula aula);

    Optional<Aula> findById(UUID id);

    List<Aula> findByCursoId(UUID cursoId);
}
