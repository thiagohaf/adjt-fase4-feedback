package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CursoJpaRepository {

    @Inject
    EntityManager entityManager;

    @Transactional
    public CursoEntity save(CursoEntity entity) {
        entityManager.persist(entity);
        return entity;
    }

    public Optional<CursoEntity> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(CursoEntity.class, id));
    }

    public List<CursoEntity> findAllOrderByCriadoEmAsc() {
        return entityManager.createQuery(
                        "SELECT c FROM CursoEntity c ORDER BY c.criadoEm ASC",
                        CursoEntity.class
                )
                .getResultList();
    }
}
