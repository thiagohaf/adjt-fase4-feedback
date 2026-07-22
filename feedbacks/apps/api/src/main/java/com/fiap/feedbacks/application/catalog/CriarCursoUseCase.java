package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Curso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class CriarCursoUseCase {

    private final CursoRepository cursoRepository;

    @Inject
    public CriarCursoUseCase(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    public Curso execute(String nome, String descricao) {
        var curso = new Curso(
                UUID.randomUUID(),
                nome.trim(),
                descricao,
                Instant.now()
        );
        return cursoRepository.save(curso);
    }
}
