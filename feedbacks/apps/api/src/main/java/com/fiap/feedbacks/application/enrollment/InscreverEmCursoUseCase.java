package com.fiap.feedbacks.application.enrollment;

import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.application.enrollment.port.InscricaoCursoRepository;
import com.fiap.feedbacks.domain.enrollment.InscricaoCurso;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
import com.fiap.feedbacks.domain.exception.InscricaoDuplicadaException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class InscreverEmCursoUseCase {

    private final CursoRepository cursoRepository;
    private final InscricaoCursoRepository inscricaoCursoRepository;
    private final CurrentUserProvider currentUserProvider;

    @Inject
    public InscreverEmCursoUseCase(
            CursoRepository cursoRepository,
            InscricaoCursoRepository inscricaoCursoRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.cursoRepository = cursoRepository;
        this.inscricaoCursoRepository = inscricaoCursoRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public InscricaoCurso execute(UUID cursoId) {
        if (cursoRepository.findById(cursoId).isEmpty()) {
            throw new CursoNotFoundException(cursoId);
        }

        UUID estudanteId = currentUserProvider.getCurrentUserId();
        if (inscricaoCursoRepository.existsByEstudanteIdAndCursoId(estudanteId, cursoId)) {
            throw new InscricaoDuplicadaException();
        }

        var inscricao = new InscricaoCurso(
                UUID.randomUUID(),
                estudanteId,
                cursoId,
                Instant.now()
        );
        return inscricaoCursoRepository.save(inscricao);
    }
}
