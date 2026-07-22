package com.fiap.feedbacks.application.catalog;

import com.fiap.feedbacks.application.catalog.port.AulaRepository;
import com.fiap.feedbacks.domain.catalog.Aula;
import com.fiap.feedbacks.domain.exception.AulaNotFoundException;
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
class ConsultarAulaUseCaseTest {

    private static final UUID AULA_ID = UUID.fromString("d4444444-4444-4444-8444-444444444444");
    private static final UUID CURSO_ID = UUID.fromString("c3333333-3333-4333-8333-333333333333");

    @Mock
    AulaRepository aulaRepository;

    ConsultarAulaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarAulaUseCase(aulaRepository);
    }

    @Test
    void encontrada_retornaAula() {
        var aula = new Aula(AULA_ID, CURSO_ID, "Aula 1", null, Instant.now());
        when(aulaRepository.findById(AULA_ID)).thenReturn(Optional.of(aula));

        assertThat(useCase.execute(AULA_ID)).isEqualTo(aula);
    }

    @Test
    void naoEncontrada_throwsAulaNotFound() {
        when(aulaRepository.findById(AULA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(AULA_ID))
                .isInstanceOf(AulaNotFoundException.class);
    }
}
