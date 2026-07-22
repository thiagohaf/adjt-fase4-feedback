package com.fiap.feedbacks.application.catalog.port;

import com.fiap.feedbacks.domain.catalog.Curso;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CursoRepository {

    Curso save(Curso curso);

    Optional<Curso> findById(UUID id);

    List<Curso> findAll();
}
