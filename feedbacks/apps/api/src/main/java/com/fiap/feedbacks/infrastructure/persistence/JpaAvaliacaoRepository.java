package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.application.avaliacao.port.AvaliacaoRepository;
import com.fiap.feedbacks.domain.avaliacao.Avaliacao;
import com.fiap.feedbacks.domain.avaliacao.Urgencia;
import com.fiap.feedbacks.domain.exception.AvaliacaoDuplicadaException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.exception.ConstraintViolationException;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JpaAvaliacaoRepository implements AvaliacaoRepository {

    private final AvaliacaoJpaRepository avaliacaoJpaRepository;

    @Inject
    public JpaAvaliacaoRepository(AvaliacaoJpaRepository avaliacaoJpaRepository) {
        this.avaliacaoJpaRepository = avaliacaoJpaRepository;
    }

    @Override
    public Avaliacao save(Avaliacao avaliacao) {
        try {
            var entity = new AvaliacaoEntity(
                    avaliacao.id(),
                    avaliacao.estudanteId(),
                    avaliacao.aulaId(),
                    avaliacao.cursoId(),
                    avaliacao.descricao(),
                    (short) avaliacao.nota(),
                    avaliacao.urgencia().name(),
                    avaliacao.ocorridoEm().atOffset(ZoneOffset.UTC)
            );
            return toDomain(avaliacaoJpaRepository.save(entity));
        } catch (RuntimeException ex) {
            if (isUniqueViolation(ex)) {
                throw new AvaliacaoDuplicadaException();
            }
            throw ex;
        }
    }

    @Override
    public boolean existsByEstudanteIdAndAulaId(UUID estudanteId, UUID aulaId) {
        return avaliacaoJpaRepository.existsByEstudanteIdAndAulaId(estudanteId, aulaId);
    }

    @Override
    public List<Avaliacao> findAll() {
        return avaliacaoJpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Avaliacao> findByEstudanteId(UUID estudanteId) {
        return avaliacaoJpaRepository.findByEstudanteId(estudanteId).stream()
                .map(this::toDomain)
                .toList();
    }

    private Avaliacao toDomain(AvaliacaoEntity entity) {
        return new Avaliacao(
                entity.getId(),
                entity.getEstudanteId(),
                entity.getAulaId(),
                entity.getCursoId(),
                entity.getDescricao(),
                entity.getNota(),
                Urgencia.valueOf(entity.getUrgencia()),
                entity.getOcorridoEm().toInstant()
        );
    }

    private static boolean isUniqueViolation(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConstraintViolationException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("uq_avaliacao_estudante_aula")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
