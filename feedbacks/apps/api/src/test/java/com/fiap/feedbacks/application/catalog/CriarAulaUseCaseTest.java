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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriarAulaUseCaseTest {

    private static final UUID CURSO_ID = UUID.fromString("b2222222-2222-4222-8222-222222222222");

    @Mock
    CursoRepository cursoRepository;
    @Mock
    AulaRepository aulaRepository;

    CriarAulaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CriarAulaUseCase(cursoRepository, aulaRepository);
    }

    @Test
    void criaAulaQuandoCursoExiste() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.of(
                new Curso(CURSO_ID, "Curso", null, Instant.now())
        ));
        when(aulaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(CURSO_ID, "Aula 1 — Containers", "Docker");

        assertThat(result.id()).isNotNull();
        assertThat(result.cursoId()).isEqualTo(CURSO_ID);
        assertThat(result.nome()).isEqualTo("Aula 1 — Containers");
        assertThat(result.descricao()).isEqualTo("Docker");
    }

    @Test
    void cursoAusente_throwsCursoNotFound() {
        when(cursoRepository.findById(CURSO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(CURSO_ID, "Aula", null))
                .isInstanceOf(CursoNotFoundException.class);

        verify(aulaRepository, never()).save(any());
    }
}
