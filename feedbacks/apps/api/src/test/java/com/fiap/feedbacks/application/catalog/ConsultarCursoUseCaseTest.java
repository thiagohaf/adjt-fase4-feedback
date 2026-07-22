package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Curso;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarCursoUseCaseTest {

    private static final UUID CURSO_ID = UUID.fromString("c3333333-3333-4333-8333-333333333333");

    @Mock
    CursoRepository cursoRepository;

    ConsultarCursoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarCursoUseCase(cursoRepository);
    }

    @Test
    void encontrado_retornaCurso() {
        var curso = new Curso(CURSO_ID, "Arquitetura", "desc", Instant.now());
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.of(curso));

        assertThat(useCase.execute(CURSO_ID)).isEqualTo(curso);
    }

    @Test
    void naoEncontrado_throwsCursoNotFound() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(CURSO_ID))
                .isInstanceOf(CursoNotFoundException.class);
    }
}
