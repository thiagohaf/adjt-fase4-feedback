package com.fiap.feedbacks.application.avaliacao;

import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.domain.auth.Papel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarAvaliacoesUseCaseTest {

    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @Mock
    CurrentUserProvider currentUserProvider;
    @Mock
    AvaliacaoReadRepository avaliacaoReadRepository;

    ListarAvaliacoesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListarAvaliacoesUseCase(currentUserProvider, avaliacaoReadRepository);
    }

    @Test
    void adminVeTodas() {
        when(currentUserProvider.getCurrentRole()).thenReturn(Papel.ADMINISTRADOR);
        when(avaliacaoReadRepository.findAll()).thenReturn(List.of(
                Map.of("id", UUID.randomUUID()),
                Map.of("id", UUID.randomUUID())
        ));

        var result = useCase.execute();

        assertThat(result).hasSize(2);
        verify(avaliacaoReadRepository).findAll();
    }

    @Test
    void estudanteVeProprias() {
        when(currentUserProvider.getCurrentRole()).thenReturn(Papel.ESTUDANTE);
        when(currentUserProvider.getCurrentUserId()).thenReturn(ESTUDANTE_ID);
        when(avaliacaoReadRepository.findByEstudanteId(ESTUDANTE_ID)).thenReturn(List.of(
                Map.of("id", UUID.randomUUID(), "estudanteId", ESTUDANTE_ID)
        ));

        var result = useCase.execute();

        assertThat(result).hasSize(1);
        verify(avaliacaoReadRepository).findByEstudanteId(ESTUDANTE_ID);
    }
}
