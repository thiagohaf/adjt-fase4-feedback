package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.AulaRepository;
import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Aula;
import com.fiap.feedbacks.domain.catalog.Curso;
import com.fiap.feedbacks.domain.exception.CursoNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarAulasDoCursoUseCaseTest {

    private static final UUID CURSO_ID = UUID.fromString("e5555555-5555-4555-8555-555555555555");

    @Mock
    CursoRepository cursoRepository;
    @Mock
    AulaRepository aulaRepository;

    ListarAulasDoCursoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListarAulasDoCursoUseCase(cursoRepository, aulaRepository);
    }

    @Test
    void listaQuandoCursoExiste() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.of(
                new Curso(CURSO_ID, "Curso", null, Instant.now())
        ));
        var aulas = List.of(new Aula(UUID.randomUUID(), CURSO_ID, "A1", null, Instant.now()));
        when(aulaRepository.findByCursoId(CURSO_ID)).thenReturn(aulas);

        assertThat(useCase.execute(CURSO_ID)).isEqualTo(aulas);
    }

    @Test
    void cursoAusente_throwsCursoNotFound() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(CURSO_ID))
                .isInstanceOf(CursoNotFoundException.class);
    }
}
