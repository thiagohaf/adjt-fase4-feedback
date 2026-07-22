package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class InscricaoAulaJpaRepository {

    @Inject
    EntityManager entityManager;

    @Transactional
    public InscricaoAulaEntity save(InscricaoAulaEntity entity) {
        entityManager.persist(entity);
        return entity;
    }

    public boolean existsByEstudanteIdAndAulaId(UUID estudanteId, UUID aulaId) {
        Long count = entityManager.createQuery(
                        """
                                SELECT COUNT(i) FROM InscricaoAulaEntity i
                                WHERE i.estudanteId = :estudanteId AND i.aulaId = :aulaId
                                """,
                        Long.class
                )
                .setParameter("estudanteId", estudanteId)
                .setParameter("aulaId", aulaId)
                .getSingleResult();
        return count != null && count > 0;
    }
}
