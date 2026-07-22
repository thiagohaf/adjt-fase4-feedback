package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.domain.avaliacao.Avaliacao;
import com.fiap.feedbacks.domain.avaliacao.Urgencia;
import com.fiap.feedbacks.domain.exception.AvaliacaoDuplicadaException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAvaliacaoRepositoryTest {

    @Mock
    AvaliacaoJpaRepository avaliacaoJpaRepository;

    JpaAvaliacaoRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaAvaliacaoRepository(avaliacaoJpaRepository);
    }

    @Test
    void savePersisteEMapeiaParaDominio() {
        UUID id = UUID.randomUUID();
        UUID estudanteId = UUID.randomUUID();
        UUID aulaId = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        Instant ocorrido = Instant.parse("2026-07-22T14:30:00Z");
        Avaliacao avaliacao = new Avaliacao(
                id, estudanteId, aulaId, cursoId, "desc", 4, Urgencia.ALTA, ocorrido
        );
        when(avaliacaoJpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Avaliacao salva = repository.save(avaliacao);

        assertThat(salva.id()).isEqualTo(id);
        assertThat(salva.estudanteId()).isEqualTo(estudanteId);
        assertThat(salva.aulaId()).isEqualTo(aulaId);
        assertThat(salva.cursoId()).isEqualTo(cursoId);
        assertThat(salva.descricao()).isEqualTo("desc");
        assertThat(salva.nota()).isEqualTo(4);
        assertThat(salva.urgencia()).isEqualTo(Urgencia.ALTA);
        assertThat(salva.ocorridoEm()).isEqualTo(ocorrido);
    }

    @Test
    void saveMapeiaUniquePorMensagem() {
        when(avaliacaoJpaRepository.save(any())).thenThrow(
                new RuntimeException(new SQLException("ERROR: duplicate key uq_avaliacao_estudante_aula"))
        );

        Avaliacao avaliacao = new Avaliacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "desc",
                3,
                Urgencia.ALTA,
                Instant.now()
        );

        assertThatThrownBy(() -> repository.save(avaliacao))
                .isInstanceOf(AvaliacaoDuplicadaException.class);
    }

    @Test
    void savePropagaErroNaoUnique() {
        when(avaliacaoJpaRepository.save(any())).thenThrow(new IllegalStateException("db down"));

        Avaliacao avaliacao = new Avaliacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "desc",
                3,
                Urgencia.ALTA,
                Instant.now()
        );

        assertThatThrownBy(() -> repository.save(avaliacao))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
    }

    @Test
    void saveMapeiaUniqueParaAvaliacaoDuplicada() {
        when(avaliacaoJpaRepository.save(any())).thenThrow(
                new RuntimeException(new ConstraintViolationException(
                        "duplicate", new SQLException("uq_avaliacao_estudante_aula"), "uq_avaliacao_estudante_aula"
                ))
        );

        Avaliacao avaliacao = new Avaliacao(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "desc",
                3,
                Urgencia.ALTA,
                Instant.now()
        );

        assertThatThrownBy(() -> repository.save(avaliacao))
                .isInstanceOf(AvaliacaoDuplicadaException.class);
    }

    @Test
    void existsEFindDelegam() {
        UUID estudanteId = UUID.randomUUID();
        UUID aulaId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        UUID cursoId = UUID.randomUUID();
        var entity = new AvaliacaoEntity(
                id, estudanteId, aulaId, cursoId, "d", (short) 8, "BAIXA",
                Instant.now().atOffset(java.time.ZoneOffset.UTC)
        );
        when(avaliacaoJpaRepository.existsByEstudanteIdAndAulaId(estudanteId, aulaId)).thenReturn(true);
        when(avaliacaoJpaRepository.findAll()).thenReturn(List.of(entity));
        when(avaliacaoJpaRepository.findByEstudanteId(estudanteId)).thenReturn(List.of(entity));

        assertThat(repository.existsByEstudanteIdAndAulaId(estudanteId, aulaId)).isTrue();
        assertThat(repository.findAll()).hasSize(1).first().extracting(Avaliacao::urgencia).isEqualTo(Urgencia.BAIXA);
        assertThat(repository.findByEstudanteId(estudanteId)).hasSize(1);
    }
}
