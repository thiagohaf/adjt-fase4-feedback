package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.application.enrollment.port.InscricaoCursoRepository;
import com.fiap.feedbacks.domain.enrollment.InscricaoCurso;
import com.fiap.feedbacks.domain.exception.InscricaoDuplicadaException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.exception.ConstraintViolationException;

import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class JpaInscricaoCursoRepository implements InscricaoCursoRepository {

    private final InscricaoCursoJpaRepository inscricaoCursoJpaRepository;

    @Inject
    public JpaInscricaoCursoRepository(InscricaoCursoJpaRepository inscricaoCursoJpaRepository) {
        this.inscricaoCursoJpaRepository = inscricaoCursoJpaRepository;
    }

    @Override
    public InscricaoCurso save(InscricaoCurso inscricao) {
        try {
            var entity = new InscricaoCursoEntity(
                    inscricao.id(),
                    inscricao.estudanteId(),
                    inscricao.cursoId(),
                    inscricao.criadoEm().atOffset(ZoneOffset.UTC)
            );
            return toDomain(inscricaoCursoJpaRepository.save(entity));
        } catch (RuntimeException ex) {
            if (isUniqueViolation(ex)) {
                throw new InscricaoDuplicadaException();
            }
            throw ex;
        }
    }

    @Override
    public boolean existsByEstudanteIdAndCursoId(UUID estudanteId, UUID cursoId) {
        return inscricaoCursoJpaRepository.existsByEstudanteIdAndCursoId(estudanteId, cursoId);
    }

    private InscricaoCurso toDomain(InscricaoCursoEntity entity) {
        return new InscricaoCurso(
                entity.getId(),
                entity.getEstudanteId(),
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
            if (message != null && message.toLowerCase().contains("uq_inscricao_curso")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
