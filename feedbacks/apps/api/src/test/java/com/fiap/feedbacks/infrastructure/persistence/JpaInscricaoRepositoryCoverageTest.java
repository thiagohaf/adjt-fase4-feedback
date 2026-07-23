package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.domain.enrollment.InscricaoAula;
import com.fiap.feedbacks.domain.enrollment.InscricaoCurso;
import com.fiap.feedbacks.domain.exception.InscricaoDuplicadaException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaInscricaoRepositoryCoverageTest {

    @Mock
    InscricaoCursoJpaRepository cursoJpa;

    @Mock
    InscricaoAulaJpaRepository aulaJpa;

    @InjectMocks
    JpaInscricaoCursoRepository cursoRepo;

    @InjectMocks
    JpaInscricaoAulaRepository aulaRepo;

    @Test
    void curso_saveSucesso() {
        UUID id = UUID.randomUUID();
        UUID estudante = UUID.randomUUID();
        UUID curso = UUID.randomUUID();
        Instant agora = Instant.parse("2026-01-01T00:00:00Z");
        when(cursoJpa.save(any())).thenAnswer(inv -> {
            InscricaoCursoEntity e = inv.getArgument(0);
            return new InscricaoCursoEntity(e.getId(), e.getEstudanteId(), e.getCursoId(), e.getCriadoEm());
        });

        InscricaoCurso saved = cursoRepo.save(new InscricaoCurso(id, estudante, curso, agora));
        assertThat(saved.id()).isEqualTo(id);
        assertThat(saved.estudanteId()).isEqualTo(estudante);
        assertThat(saved.cursoId()).isEqualTo(curso);
    }

    @Test
    void curso_uniqueConstraintMapeiaDuplicada() {
        when(cursoJpa.save(any())).thenThrow(
                new ConstraintViolationException("dup", new SQLException("x"), "uq_inscricao_curso"));
        assertThatThrownBy(() -> cursoRepo.save(sampleCurso()))
                .isInstanceOf(InscricaoDuplicadaException.class);
    }

    @Test
    void curso_uniquePorMensagemMapeiaDuplicada() {
        when(cursoJpa.save(any())).thenThrow(new RuntimeException("violates UQ_INSCRICAO_CURSO"));
        assertThatThrownBy(() -> cursoRepo.save(sampleCurso()))
                .isInstanceOf(InscricaoDuplicadaException.class);
    }

    @Test
    void curso_outraRuntimePropaga() {
        when(cursoJpa.save(any())).thenThrow(new IllegalStateException("db down"));
        assertThatThrownBy(() -> cursoRepo.save(sampleCurso()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db down");
    }

    @Test
    void curso_runtimeComMensagemNullNaoEDuplicada() {
        when(cursoJpa.save(any())).thenThrow(new RuntimeException((String) null));
        assertThatThrownBy(() -> cursoRepo.save(sampleCurso()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(null);
    }

    @Test
    void aula_runtimeComMensagemNullNaoEDuplicada() {
        when(aulaJpa.save(any())).thenThrow(new RuntimeException((String) null));
        assertThatThrownBy(() -> aulaRepo.save(sampleAula(UUID.randomUUID())))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void curso_existsDelega() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(cursoJpa.existsByEstudanteIdAndCursoId(a, b)).thenReturn(true);
        assertThat(cursoRepo.existsByEstudanteIdAndCursoId(a, b)).isTrue();
    }

    @Test
    void aula_saveSucesso() {
        UUID id = UUID.randomUUID();
        when(aulaJpa.save(any())).thenAnswer(inv -> {
            InscricaoAulaEntity e = inv.getArgument(0);
            return new InscricaoAulaEntity(
                    e.getId(), e.getEstudanteId(), e.getAulaId(), e.getCursoId(), e.getCriadoEm());
        });
        InscricaoAula saved = aulaRepo.save(sampleAula(id));
        assertThat(saved.id()).isEqualTo(id);
    }

    @Test
    void aula_uniqueConstraintMapeiaDuplicada() {
        when(aulaJpa.save(any())).thenThrow(
                new ConstraintViolationException("dup", new SQLException("x"), "uq_inscricao_aula"));
        assertThatThrownBy(() -> aulaRepo.save(sampleAula(UUID.randomUUID())))
                .isInstanceOf(InscricaoDuplicadaException.class);
    }

    @Test
    void aula_uniquePorMensagemMapeiaDuplicada() {
        when(aulaJpa.save(any())).thenThrow(new RuntimeException(new RuntimeException("UQ_INSCRICAO_AULA")));
        assertThatThrownBy(() -> aulaRepo.save(sampleAula(UUID.randomUUID())))
                .isInstanceOf(InscricaoDuplicadaException.class);
    }

    @Test
    void aula_outraRuntimePropaga() {
        when(aulaJpa.save(any())).thenThrow(new IllegalStateException("boom"));
        assertThatThrownBy(() -> aulaRepo.save(sampleAula(UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aula_existsDelega() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(aulaJpa.existsByEstudanteIdAndAulaId(a, b)).thenReturn(false);
        assertThat(aulaRepo.existsByEstudanteIdAndAulaId(a, b)).isFalse();
    }

    private static InscricaoCurso sampleCurso() {
        return new InscricaoCurso(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    }

    private static InscricaoAula sampleAula(UUID id) {
        return new InscricaoAula(id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());
    }
}
