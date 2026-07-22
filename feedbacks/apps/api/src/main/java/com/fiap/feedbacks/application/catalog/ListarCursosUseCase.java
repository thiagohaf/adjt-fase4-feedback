package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Curso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ListarCursosUseCase {

    private final CursoRepository cursoRepository;

    @Inject
    public ListarCursosUseCase(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public List<Curso> execute() {
        return cursoRepository.findAll();
    }
}
