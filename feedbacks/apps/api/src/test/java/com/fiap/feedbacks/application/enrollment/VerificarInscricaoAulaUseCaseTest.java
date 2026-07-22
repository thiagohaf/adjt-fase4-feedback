package com.fiap.feedbacks.application.enrollment;

import com.fiap.feedbacks.application.enrollment.port.InscricaoAulaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificarInscricaoAulaUseCaseTest {

    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");
    private static final UUID AULA_ID = UUID.fromString("d4444444-4444-4444-8444-444444444444");

    @Mock
    InscricaoAulaRepository inscricaoAulaRepository;

    VerificarInscricaoAulaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerificarInscricaoAulaUseCase(inscricaoAulaRepository);
    }

    @Test
    void retornaTrueQuandoInscrito() {
        when(inscricaoAulaRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID))
                .thenReturn(true);

        assertThat(useCase.execute(ESTUDANTE_ID, AULA_ID)).isTrue();
    }

    @Test
    void retornaFalseQuandoNaoInscrito() {
        when(inscricaoAulaRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID))
                .thenReturn(false);

        assertThat(useCase.execute(ESTUDANTE_ID, AULA_ID)).isFalse();
    }
}
