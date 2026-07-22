package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.application.avaliacao.AvaliacaoReadRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaAvaliacaoReadRepository implements AvaliacaoReadRepository {

    private final AvaliacaoJpaRepository avaliacaoJpaRepository;

    @Inject
    public JpaAvaliacaoReadRepository(AvaliacaoJpaRepository avaliacaoJpaRepository) {
        this.avaliacaoJpaRepository = avaliacaoJpaRepository;
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return avaliacaoJpaRepository.findAll().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> findByEstudanteId(UUID estudanteId) {
        return avaliacaoJpaRepository.findByEstudanteId(estudanteId).stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toMap(AvaliacaoEntity entity) {
        return Map.of(
                "id", entity.getId(),
                "estudanteId", entity.getEstudanteId(),
                "descricao", entity.getDescricao()
        );
    }
}
