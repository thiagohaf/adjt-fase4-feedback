package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AulaJpaRepository {

    @Inject
    EntityManager entityManager;

    @Transactional
    public AulaEntity save(AulaEntity entity) {
        entityManager.persist(entity);
        return entity;
    }

    public Optional<AulaEntity> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(AulaEntity.class, id));
    }

    public List<AulaEntity> findByCursoIdOrderByCriadoEmAsc(UUID cursoId) {
        return entityManager.createQuery(
                        "SELECT a FROM AulaEntity a WHERE a.cursoId = :cursoId ORDER BY a.criadoEm ASC",
                        AulaEntity.class
                )
                .setParameter("cursoId", cursoId)
                .getResultList();
    }
}
