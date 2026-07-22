package com.fiap.feedbacks.application.enrollment;

import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.application.catalog.port.AulaRepository;
import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.application.enrollment.port.InscricaoAulaRepository;
import com.fiap.feedbacks.application.enrollment.port.InscricaoCursoRepository;
import com.fiap.feedbacks.domain.catalog.Aula;
import com.fiap.feedbacks.domain.enrollment.InscricaoAula;
import com.fiap.feedbacks.domain.exception.AulaNotFoundException;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
import com.fiap.feedbacks.domain.exception.InscricaoCursoObrigatoriaException;
import com.fiap.feedbacks.domain.exception.InscricaoDuplicadaException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class InscreverEmAulaUseCase {

    private final CursoRepository cursoRepository;
    private final AulaRepository aulaRepository;
    private final InscricaoCursoRepository inscricaoCursoRepository;
    private final InscricaoAulaRepository inscricaoAulaRepository;
    private final CurrentUserProvider currentUserProvider;

    @Inject
    public InscreverEmAulaUseCase(
            CursoRepository cursoRepository,
            AulaRepository aulaRepository,
            InscricaoCursoRepository inscricaoCursoRepository,
            InscricaoAulaRepository inscricaoAulaRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.cursoRepository = cursoRepository;
        this.aulaRepository = aulaRepository;
        this.inscricaoCursoRepository = inscricaoCursoRepository;
        this.inscricaoAulaRepository = inscricaoAulaRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public InscricaoAula execute(UUID cursoId, UUID aulaId) {
        if (cursoRepository.findById(cursoId).isEmpty()) {
            throw new CursoNotFoundException(cursoId);
        }

        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new AulaNotFoundException(aulaId));
        if (!aula.cursoId().equals(cursoId)) {
            throw new AulaNotFoundException(aulaId);
        }

        UUID estudanteId = currentUserProvider.getCurrentUserId();
        if (!inscricaoCursoRepository.existsByEstudanteIdAndCursoId(estudanteId, cursoId)) {
            throw new InscricaoCursoObrigatoriaException(cursoId);
        }
        if (inscricaoAulaRepository.existsByEstudanteIdAndAulaId(estudanteId, aulaId)) {
            throw new InscricaoDuplicadaException();
        }

        var inscricao = new InscricaoAula(
                UUID.randomUUID(),
                estudanteId,
                aulaId,
                cursoId,
                Instant.now()
        );
        return inscricaoAulaRepository.save(inscricao);
    }
}
