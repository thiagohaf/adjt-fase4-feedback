package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AvaliacaoJpaRepository {

    @Inject
    EntityManager entityManager;

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
