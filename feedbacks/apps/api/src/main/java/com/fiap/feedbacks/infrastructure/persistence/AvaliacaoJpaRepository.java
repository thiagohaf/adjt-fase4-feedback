package com.fiap.feedbacks.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvaliacaoJpaRepository extends JpaRepository<AvaliacaoEntity, UUID> {

    List<AvaliacaoEntity> findByEstudanteId(UUID estudanteId);
}
