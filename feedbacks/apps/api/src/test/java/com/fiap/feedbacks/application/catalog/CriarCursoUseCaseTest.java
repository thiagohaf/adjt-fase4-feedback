package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.CursoRepository;
import com.fiap.feedbacks.domain.catalog.Curso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriarCursoUseCaseTest {

    @Mock
    CursoRepository cursoRepository;

    CriarCursoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CriarCursoUseCase(cursoRepository);
    }

    @Test
    void criaCursoComNomeEDescricao() {
        when(cursoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute("Arquitetura Cloud", "Curso introdutório");

        assertThat(result.id()).isNotNull();
        assertThat(result.nome()).isEqualTo("Arquitetura Cloud");
        assertThat(result.descricao()).isEqualTo("Curso introdutório");
        assertThat(result.criadoEm()).isNotNull();

        ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
        verify(cursoRepository).save(captor.capture());
        assertThat(captor.getValue().nome()).isEqualTo("Arquitetura Cloud");
    }

    @Test
    void criaCursoTrimNomeEDescricaoNull() {
        when(cursoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute("  Somente Nome  ", null);

        assertThat(result.nome()).isEqualTo("Somente Nome");
        assertThat(result.descricao()).isNull();
    }
}
