package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.AulaRepository;
import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Aula;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class CriarAulaUseCase {

    private final CursoRepository cursoRepository;
    private final AulaRepository aulaRepository;

    @Inject
    public CriarAulaUseCase(CursoRepository cursoRepository, AulaRepository aulaRepository) {
        this.cursoRepository = cursoRepository;
        this.aulaRepository = aulaRepository;
    }

    public Aula execute(UUID cursoId, String nome, String descricao) {
        if (cursoRepository.findById(cursoId).isEmpty()) {
            throw new CursoNotFoundException(cursoId);
        }

        var aula = new Aula(
                UUID.randomUUID(),
                cursoId,
                nome.trim(),
                descricao,
                Instant.now()
        );
        return aulaRepository.save(aula);
    }
}
