package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class InscricaoCursoJpaRepository {

    @Inject
    EntityManager entityManager;

    @Transactional
    public InscricaoCursoEntity save(InscricaoCursoEntity entity) {
        entityManager.persist(entity);
        return entity;
    }

    public boolean existsByEstudanteIdAndCursoId(UUID estudanteId, UUID cursoId) {
        Long count = entityManager.createQuery(
                        """
                                SELECT COUNT(i) FROM InscricaoCursoEntity i
                                WHERE i.estudanteId = :estudanteId AND i.cursoId = :cursoId
                                """,
                        Long.class
                )
                .setParameter("estudanteId", estudanteId)
                .setParameter("cursoId", cursoId)
                .getSingleResult();
        return count != null && count > 0;
    }
}
