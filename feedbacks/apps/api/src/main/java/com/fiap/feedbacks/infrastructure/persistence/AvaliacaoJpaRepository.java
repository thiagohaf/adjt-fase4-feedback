package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AvaliacaoJpaRepository {

    @Inject
    EntityManager entityManager;

    @Transactional
    public AvaliacaoEntity save(AvaliacaoEntity entity) {
        entityManager.persist(entity);
        return entity;
    }

    public boolean existsByEstudanteIdAndAulaId(UUID estudanteId, UUID aulaId) {
        Long count = entityManager.createQuery(
                        """
                                SELECT COUNT(a) FROM AvaliacaoEntity a
                                WHERE a.estudanteId = :estudanteId AND a.aulaId = :aulaId
                                """,
                        Long.class
                )
                .setParameter("estudanteId", estudanteId)
                .setParameter("aulaId", aulaId)
                .getSingleResult();
        return count != null && count > 0;
    }

    public List<AvaliacaoEntity> findAll() {
        return entityManager.createQuery(
                        "SELECT a FROM AvaliacaoEntity a",
                        AvaliacaoEntity.class
                )
                .getResultList();
    }

    public List<AvaliacaoEntity> findByEstudanteId(UUID estudanteId) {
        return entityManager.createQuery(
                        "SELECT a FROM AvaliacaoEntity a WHERE a.estudanteId = :estudanteId",
                        AvaliacaoEntity.class
                )
                .setParameter("estudanteId", estudanteId)
                .getResultList();
    }
}
