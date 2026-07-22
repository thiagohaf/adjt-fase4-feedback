package com.fiap.feedbacks.application.enrollment;

import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.application.enrollment.port.InscricaoCursoRepository;
import com.fiap.feedbacks.domain.catalog.Curso;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
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
class InscreverEmCursoUseCaseTest {

    private static final UUID CURSO_ID = UUID.fromString("b2222222-2222-4222-8222-222222222222");
    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @Mock
    CursoRepository cursoRepository;
    @Mock
    InscricaoCursoRepository inscricaoCursoRepository;
    @Mock
    CurrentUserProvider currentUserProvider;

    InscreverEmCursoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new InscreverEmCursoUseCase(
                cursoRepository,
                inscricaoCursoRepository,
                currentUserProvider
        );
    }

    @Test
    void inscreveQuandoCursoExiste() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.of(
                new Curso(CURSO_ID, "Curso", null, Instant.now())
        ));
        when(currentUserProvider.getCurrentUserId()).thenReturn(ESTUDANTE_ID);
        when(inscricaoCursoRepository.existsByEstudanteIdAndCursoId(ESTUDANTE_ID, CURSO_ID))
                .thenReturn(false);
        when(inscricaoCursoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(CURSO_ID);

        assertThat(result.id()).isNotNull();
        assertThat(result.cursoId()).isEqualTo(CURSO_ID);
        assertThat(result.estudanteId()).isEqualTo(ESTUDANTE_ID);
    }

    @Test
    void cursoAusente_throwsCursoNotFound() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(CURSO_ID))
                .isInstanceOf(CursoNotFoundException.class);

        verify(inscricaoCursoRepository, never()).save(any());
    }

    @Test
    void duplicata_throwsInscricaoDuplicada() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.of(
                new Curso(CURSO_ID, "Curso", null, Instant.now())
        ));
        when(currentUserProvider.getCurrentUserId()).thenReturn(ESTUDANTE_ID);
        when(inscricaoCursoRepository.existsByEstudanteIdAndCursoId(ESTUDANTE_ID, CURSO_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(CURSO_ID))
                .isInstanceOf(InscricaoDuplicadaException.class);

        verify(inscricaoCursoRepository, never()).save(any());
    }
}
