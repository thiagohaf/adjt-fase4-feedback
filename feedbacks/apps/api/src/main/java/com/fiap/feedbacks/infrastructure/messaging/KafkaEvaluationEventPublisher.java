package com.fiap.feedbacks.infrastructure.messaging;

import com.fiap.feedbacks.application.avaliacao.AvaliacaoAlertaEvent;
import com.fiap.feedbacks.application.avaliacao.port.EvaluationEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Adapter Kafka (%local) — serializa payload AD-5. Sem broker no MVP: no-op seguro com log.
 * Ativo em builds que não são {@code test} nem {@code aws}.
 */
@ApplicationScoped
@UnlessBuildProfile(anyOf = {"test", "aws"})
public class KafkaEvaluationEventPublisher implements EvaluationEventPublisher {

    private static final Logger LOG = Logger.getLogger(KafkaEvaluationEventPublisher.class);
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;

    @Inject
    public KafkaEvaluationEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(AvaliacaoAlertaEvent event) {
        String payload = toJson(event);
        LOG.infof("KafkaEvaluationEventPublisher (no-op sem broker): topic=avaliacao-alerta payload=%s", payload);
    }

    private String toJson(AvaliacaoAlertaEvent event) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("avaliacaoId", event.avaliacaoId().toString());
            node.put("descricao", event.descricao());
            node.put("urgencia", event.urgencia().name());
            node.put("ocorridoEm", ISO_OFFSET.format(event.ocorridoEm().atOffset(ZoneOffset.UTC)));
            node.put("aulaId", event.aulaId().toString());
            node.put("cursoId", event.cursoId().toString());
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar evento AD-5", ex);
        }
    }
}
