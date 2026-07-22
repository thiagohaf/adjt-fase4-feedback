package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.AulaRepository;
import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Aula;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListarAulasDoCursoUseCase {

    private final CursoRepository cursoRepository;
    private final AulaRepository aulaRepository;

    @Inject
    public ListarAulasDoCursoUseCase(CursoRepository cursoRepository, AulaRepository aulaRepository) {
        this.cursoRepository = cursoRepository;
        this.aulaRepository = aulaRepository;
    }

    public List<Aula> execute(UUID cursoId) {
        if (cursoRepository.findById(cursoId).isEmpty()) {
            throw new CursoNotFoundException(cursoId);
        }
        return aulaRepository.findByCursoId(cursoId);
    }
}
