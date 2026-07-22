package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Curso;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class ConsultarCursoUseCase {

    private final CursoRepository cursoRepository;

    @Inject
    public ConsultarCursoUseCase(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public Curso execute(UUID id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new CursoNotFoundException(id));
    }
}
