package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.Optional;

@ApplicationScoped
public class UsuarioJpaRepository {

    @Inject
    EntityManager entityManager;

    public Optional<UsuarioEntity> findByEmail(String email) {
        return entityManager.createQuery(
                        "SELECT u FROM UsuarioEntity u WHERE u.email = :email",
                        UsuarioEntity.class
                )
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }
}
