package com.fiap.feedbacks.application.avaliacao;

import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.application.avaliacao.port.AvaliacaoRepository;
import com.fiap.feedbacks.application.avaliacao.port.EvaluationEventPublisher;
import com.fiap.feedbacks.application.catalog.port.AulaRepository;
import com.fiap.feedbacks.application.enrollment.VerificarInscricaoAulaUseCase;
import com.fiap.feedbacks.domain.avaliacao.Avaliacao;
import com.fiap.feedbacks.domain.avaliacao.Urgencia;
import com.fiap.feedbacks.domain.catalog.Aula;
import com.fiap.feedbacks.domain.exception.AulaNotFoundException;
import com.fiap.feedbacks.domain.exception.AvaliacaoDuplicadaException;
import com.fiap.feedbacks.domain.exception.InscricaoAulaObrigatoriaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriarAvaliacaoUseCaseTest {

    private static final UUID ESTUDANTE_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");
    private static final UUID AULA_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID CURSO_ID = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

    @Mock
    CurrentUserProvider currentUserProvider;
    @Mock
    AulaRepository aulaRepository;
    @Mock
    VerificarInscricaoAulaUseCase verificarInscricaoAulaUseCase;
    @Mock
    AvaliacaoRepository avaliacaoRepository;
    @Mock
    EvaluationEventPublisher evaluationEventPublisher;

    CriarAvaliacaoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CriarAvaliacaoUseCase(
                currentUserProvider,
                aulaRepository,
                verificarInscricaoAulaUseCase,
                avaliacaoRepository,
                evaluationEventPublisher
        );
    }

    @Test
    void sucessoPersisteERetornaAvaliacao() {
        stubAulaInscrita();
        when(avaliacaoRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID)).thenReturn(false);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Avaliacao result = useCase.execute(AULA_ID, "Feedback", 6);

        assertThat(result.urgencia()).isEqualTo(Urgencia.MEDIA);
        assertThat(result.cursoId()).isEqualTo(CURSO_ID);
        assertThat(result.estudanteId()).isEqualTo(ESTUDANTE_ID);
        verify(avaliacaoRepository).save(any(Avaliacao.class));
        verify(evaluationEventPublisher, never()).publish(any());
    }

    @Test
    void duplicataLancaAvaliacaoDuplicada() {
        stubAulaInscrita();
        when(avaliacaoRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(AULA_ID, "Feedback", 5))
                .isInstanceOf(AvaliacaoDuplicadaException.class);
        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void semInscricaoLanca403Domain() {
        when(aulaRepository.findById(AULA_ID)).thenReturn(Optional.of(aula()));
        when(currentUserProvider.getCurrentUserId()).thenReturn(ESTUDANTE_ID);
        when(verificarInscricaoAulaUseCase.execute(ESTUDANTE_ID, AULA_ID)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(AULA_ID, "Feedback", 5))
                .isInstanceOf(InscricaoAulaObrigatoriaException.class);
    }

    @Test
    void aulaInexistenteLanca404() {
        when(aulaRepository.findById(AULA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(AULA_ID, "Feedback", 5))
                .isInstanceOf(AulaNotFoundException.class);
    }

    @Test
    void notaInvalidaRejeitadaNoDominio() {
        stubAulaInscrita();
        when(avaliacaoRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(AULA_ID, "Feedback", 11))
                .isInstanceOf(IllegalArgumentException.class);
        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    void urgenciaAltaPublicaEvento() {
        stubAulaInscrita();
        when(avaliacaoRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID)).thenReturn(false);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));

        Avaliacao result = useCase.execute(AULA_ID, "Ruim", 3);

        ArgumentCaptor<AvaliacaoAlertaEvent> captor = ArgumentCaptor.forClass(AvaliacaoAlertaEvent.class);
        verify(evaluationEventPublisher).publish(captor.capture());
        AvaliacaoAlertaEvent event = captor.getValue();
        assertThat(event.avaliacaoId()).isEqualTo(result.id());
        assertThat(event.urgencia()).isEqualTo(Urgencia.ALTA);
        assertThat(event.descricao()).isEqualTo("Ruim");
        assertThat(event.aulaId()).isEqualTo(AULA_ID);
        assertThat(event.cursoId()).isEqualTo(CURSO_ID);
        assertThat(event.ocorridoEm()).isNotNull();
    }

    @Test
    void urgenciaBaixaNaoPublica() {
        stubAulaInscrita();
        when(avaliacaoRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID)).thenReturn(false);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(AULA_ID, "Ótimo", 9);

        verify(evaluationEventPublisher, never()).publish(any());
    }

    @Test
    void falhaPublishNaoReverteAvaliacao() {
        stubAulaInscrita();
        when(avaliacaoRepository.existsByEstudanteIdAndAulaId(ESTUDANTE_ID, AULA_ID)).thenReturn(false);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("broker down")).when(evaluationEventPublisher).publish(any());

        Avaliacao result = useCase.execute(AULA_ID, "Ruim", 2);

        assertThat(result.urgencia()).isEqualTo(Urgencia.ALTA);
        verify(avaliacaoRepository).save(any(Avaliacao.class));
    }

    private void stubAulaInscrita() {
        when(aulaRepository.findById(AULA_ID)).thenReturn(Optional.of(aula()));
        when(currentUserProvider.getCurrentUserId()).thenReturn(ESTUDANTE_ID);
        when(verificarInscricaoAulaUseCase.execute(ESTUDANTE_ID, AULA_ID)).thenReturn(true);
    }

    private static Aula aula() {
        return new Aula(AULA_ID, CURSO_ID, "Aula", "desc", Instant.now());
    }
}
