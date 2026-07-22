package com.fiap.feedbacks.application.enrollment;

import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.application.catalog.port.AulaRepository;
import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.application.enrollment.port.InscricaoAulaRepository;
import com.fiap.feedbacks.application.enrollment.port.InscricaoCursoRepository;
import com.fiap.feedbacks.domain.catalog.Aula;
import com.fiap.feedbacks.domain.catalog.Curso;
import com.fiap.feedbacks.domain.exception.AulaNotFoundException;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
import com.fiap.feedbacks.domain.exception.InscricaoCursoObrigatoriaException;
import com.fiap.feedbacks.domain.exception.InscricaoDuplicadaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscreverEmAulaUseCaseTest {

    private static final UUID CURSO_ID = UUID.fromString("b2222222-2222-4222-8222-222222222222");
    private static final UUID OUTRO_CURSO_ID = UUID.fromString("c3333333-3333-4333-8333-333333333333");
    private static final UUID AULA_ID = UUID.fromString("d4444444-4444-4444-8444-444444444444");
    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @Mock
    CursoRepository cursoRepository;
    @Mock
    AulaRepository aulaRepository;
    @Mock
    InscricaoCursoRepository inscricaoCursoRepository;
    @Mock
    InscricaoAulaRepository inscricaoAulaRepository;
    @Mock
    CurrentUserProvider currentUserProvider;

    InscreverEmAulaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new InscreverEmAulaUseCase(
                cursoRepository,
                aulaRepository,
                inscricaoCursoRepository,
                inscricaoAulaRepository,
                currentUserProvider
        );
    }

    @Test
    void inscreveQuandoPrecondicoesOk() {
        stubCursoEAulaDoCurso();
        when(currentUserProvider.getCurrentUserId()).thenReturn(ESTUDANTE_ID);
        when(inscricaoCursoRepository.existsByEstudanteIdAndCursoId(ESTUDANTE_ID, CURSO_ID))
                .thenReturn(true);
        when(inscricaoAulaRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID))
                .thenReturn(false);
        when(inscricaoAulaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(CURSO_ID, AULA_ID);

        assertThat(result.id()).isNotNull();
        assertThat(result.cursoId()).isEqualTo(CURSO_ID);
        assertThat(result.aulaId()).isEqualTo(AULA_ID);
        assertThat(result.estudanteId()).isEqualTo(ESTUDANTE_ID);
    }

    @Test
    void cursoAusente_throwsCursoNotFound() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(CURSO_ID, AULA_ID))
                .isInstanceOf(CursoNotFoundException.class);

        verify(inscricaoAulaRepository, never()).save(any());
    }

    @Test
    void aulaDeOutroCurso_throwsAulaNotFound() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.of(
                new Curso(CURSO_ID, "Curso", null, Instant.now())
        ));
        when(aulaRepository.findById(AULA_ID)).thenReturn(Optional.of(
                new Aula(AULA_ID, OUTRO_CURSO_ID, "Aula", null, Instant.now())
        ));

        assertThatThrownBy(() -> useCase.execute(CURSO_ID, AULA_ID))
                .isInstanceOf(AulaNotFoundException.class);

        verify(inscricaoAulaRepository, never()).save(any());
    }

    @Test
    void semInscricaoNoCurso_throwsInscricaoCursoObrigatoria() {
        stubCursoEAulaDoCurso();
        when(currentUserProvider.getCurrentUserId()).thenReturn(ESTUDANTE_ID);
        when(inscricaoCursoRepository.existsByEstudanteIdAndCursoId(ESTUDANTE_ID, CURSO_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(CURSO_ID, AULA_ID))
                .isInstanceOf(InscricaoCursoObrigatoriaException.class);

        verify(inscricaoAulaRepository, never()).save(any());
    }

    @Test
    void duplicata_throwsInscricaoDuplicada() {
        stubCursoEAulaDoCurso();
        when(currentUserProvider.getCurrentUserId()).thenReturn(ESTUDANTE_ID);
        when(inscricaoCursoRepository.existsByEstudanteIdAndCursoId(ESTUDANTE_ID, CURSO_ID))
                .thenReturn(true);
        when(inscricaoAulaRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(CURSO_ID, AULA_ID))
                .isInstanceOf(InscricaoDuplicadaException.class);

        verify(inscricaoAulaRepository, never()).save(any());
    }

    private void stubCursoEAulaDoCurso() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.of(
                new Curso(CURSO_ID, "Curso", null, Instant.now())
        ));
        when(aulaRepository.findById(AULA_ID)).thenReturn(Optional.of(
                new Aula(AULA_ID, CURSO_ID, "Aula", null, Instant.now())
        ));
    }
}
