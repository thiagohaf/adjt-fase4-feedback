package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Curso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaCursoRepository implements CursoRepository {

    private final CursoJpaRepository cursoJpaRepository;

    @Inject
    public JpaCursoRepository(CursoJpaRepository cursoJpaRepository) {
        this.cursoJpaRepository = cursoJpaRepository;
    }

    @Override
    public Curso save(Curso curso) {
        var entity = new CursoEntity(
                curso.id(),
                curso.nome(),
                curso.descricao(),
                curso.criadoEm().atOffset(ZoneOffset.UTC)
        );
        return toDomain(cursoJpaRepository.save(entity));
    }

    @Override
    public Optional<Curso> findById(UUID id) {
        return cursoJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Curso> findAll() {
        return cursoJpaRepository.findAllOrderByCriadoEmAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    private Curso toDomain(CursoEntity entity) {
        return new Curso(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getCriadoEm().toInstant()
        );
    }
}
