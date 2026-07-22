package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.application.catalog.port.AulaRepository;
import com.fiap.feedbacks.domain.catalog.Aula;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaAulaRepository implements AulaRepository {

    private final AulaJpaRepository aulaJpaRepository;

    @Inject
    public JpaAulaRepository(AulaJpaRepository aulaJpaRepository) {
        this.aulaJpaRepository = aulaJpaRepository;
    }

    @Override
    public Aula save(Aula aula) {
        var entity = new AulaEntity(
                aula.id(),
                aula.cursoId(),
                aula.nome(),
                aula.descricao(),
                aula.criadoEm().atOffset(ZoneOffset.UTC)
        );
        return toDomain(aulaJpaRepository.save(entity));
    }

    @Override
    public Optional<Aula> findById(UUID id) {
        return aulaJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Aula> findByCursoId(UUID cursoId) {
        return aulaJpaRepository.findByCursoIdOrderByCriadoEmAsc(cursoId).stream()
                .map(this::toDomain)
                .toList();
    }

    private Aula toDomain(AulaEntity entity) {
        return new Aula(
                entity.getId(),
                entity.getCursoId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getCriadoEm().toInstant()
        );
    }
}
