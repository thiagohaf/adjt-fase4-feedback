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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class CriarAvaliacaoUseCase {

    private static final Logger LOG = Logger.getLogger(CriarAvaliacaoUseCase.class);

    private final CurrentUserProvider currentUserProvider;
    private final AulaRepository aulaRepository;
    private final VerificarInscricaoAulaUseCase verificarInscricaoAulaUseCase;
    private final AvaliacaoRepository avaliacaoRepository;
    private final EvaluationEventPublisher evaluationEventPublisher;

    @Inject
    public CriarAvaliacaoUseCase(
            CurrentUserProvider currentUserProvider,
            AulaRepository aulaRepository,
            VerificarInscricaoAulaUseCase verificarInscricaoAulaUseCase,
            AvaliacaoRepository avaliacaoRepository,
            EvaluationEventPublisher evaluationEventPublisher
    ) {
        this.currentUserProvider = currentUserProvider;
        this.aulaRepository = aulaRepository;
        this.verificarInscricaoAulaUseCase = verificarInscricaoAulaUseCase;
        this.avaliacaoRepository = avaliacaoRepository;
        this.evaluationEventPublisher = evaluationEventPublisher;
    }

    public Avaliacao execute(UUID aulaId, String descricao, int nota) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new AulaNotFoundException(aulaId));

        UUID estudanteId = currentUserProvider.getCurrentUserId();
        if (!verificarInscricaoAulaUseCase.execute(estudanteId, aulaId)) {
            throw new InscricaoAulaObrigatoriaException(aulaId);
        }
        if (avaliacaoRepository.existsByEstudanteIdAndAulaId(estudanteId, aulaId)) {
            throw new AvaliacaoDuplicadaException();
        }

        Avaliacao avaliacao = Avaliacao.criar(
                estudanteId,
                aulaId,
                aula.cursoId(),
                descricao,
                nota
        );
        Avaliacao salva = avaliacaoRepository.save(avaliacao);

        if (salva.urgencia() == Urgencia.ALTA) {
            try {
                evaluationEventPublisher.publish(AvaliacaoAlertaEvent.from(salva));
            } catch (RuntimeException ex) {
                LOG.warnf(ex, "Falha ao publicar evento de alerta ALTA para avaliacaoId=%s", salva.id());
            }
        }
        return salva;
    }
}
