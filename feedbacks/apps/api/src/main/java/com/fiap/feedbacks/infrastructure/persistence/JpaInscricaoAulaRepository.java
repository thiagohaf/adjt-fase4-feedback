package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.application.enrollment.port.InscricaoAulaRepository;
import com.fiap.feedbacks.domain.enrollment.InscricaoAula;
import com.fiap.feedbacks.domain.exception.InscricaoDuplicadaException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.exception.ConstraintViolationException;

import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class JpaInscricaoAulaRepository implements InscricaoAulaRepository {

    private final InscricaoAulaJpaRepository inscricaoAulaJpaRepository;

    @Inject
    public JpaInscricaoAulaRepository(InscricaoAulaJpaRepository inscricaoAulaJpaRepository) {
        this.inscricaoAulaJpaRepository = inscricaoAulaJpaRepository;
    }

    @Override
    public InscricaoAula save(InscricaoAula inscricao) {
        try {
            var entity = new InscricaoAulaEntity(
                    inscricao.id(),
                    inscricao.estudanteId(),
                    inscricao.aulaId(),
                    inscricao.cursoId(),
                    inscricao.criadoEm().atOffset(ZoneOffset.UTC)
            );
            return toDomain(inscricaoAulaJpaRepository.save(entity));
        } catch (RuntimeException ex) {
            if (isUniqueViolation(ex)) {
                throw new InscricaoDuplicadaException();
            }
            throw ex;
        }
    }

    @Override
    public boolean existsByEstudanteIdAndAulaId(UUID estudanteId, UUID aulaId) {
        return inscricaoAulaJpaRepository.existsByEstudanteIdAndAulaId(estudanteId, aulaId);
    }

    private InscricaoAula toDomain(InscricaoAulaEntity entity) {
        return new InscricaoAula(
                entity.getId(),
                entity.getEstudanteId(),
                entity.getAulaId(),
                entity.getCursoId(),
                entity.getCriadoEm().toInstant()
        );
    }

    private static boolean isUniqueViolation(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConstraintViolationException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("uq_inscricao_aula")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
