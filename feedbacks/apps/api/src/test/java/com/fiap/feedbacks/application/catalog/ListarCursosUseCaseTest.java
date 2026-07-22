package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Curso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarCursosUseCaseTest {

    @Mock
    CursoRepository cursoRepository;

    ListarCursosUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListarCursosUseCase(cursoRepository);
    }

    @Test
    void retornaListaDoRepositorio() {
        var cursos = List.of(new Curso(UUID.randomUUID(), "C1", null, Instant.now()));
        when(cursoRepository.findAll()).thenReturn(cursos);

        assertThat(useCase.execute()).isEqualTo(cursos);
    }
}
