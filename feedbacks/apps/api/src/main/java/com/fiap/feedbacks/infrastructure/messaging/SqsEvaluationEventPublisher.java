package com.fiap.feedbacks.infrastructure.messaging;

import com.fiap.feedbacks.application.avaliacao.AvaliacaoAlertaEvent;
import com.fiap.feedbacks.application.avaliacao.port.EvaluationEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

/**
 * Adapter SQS (%aws) — publish real quando {@code feedbacks.messaging.sqs.queue-url} está
 * definido; no-op seguro com log se ausente (CI / sem fila).
 */
@ApplicationScoped
@IfBuildProfile("aws")
public class SqsEvaluationEventPublisher implements EvaluationEventPublisher {

    private static final Logger LOG = Logger.getLogger(SqsEvaluationEventPublisher.class);
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;
    private final String queueUrl;
    private final BiConsumer<String, String> messageSender;

    @Inject
    public SqsEvaluationEventPublisher(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "feedbacks.messaging.sqs.queue-url", defaultValue = "") String queueUrl) {
        this(objectMapper, queueUrl, defaultSender());
    }

    SqsEvaluationEventPublisher(
            ObjectMapper objectMapper,
            String queueUrl,
            BiConsumer<String, String> messageSender) {
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl == null ? "" : queueUrl.trim();
        this.messageSender = messageSender;
    }

    @Override
    public void publish(AvaliacaoAlertaEvent event) {
        String payload = toJson(event);
        if (queueUrl.isBlank()) {
            LOG.infof("SqsEvaluationEventPublisher (no-op — queue-url ausente): payload=%s", payload);
            return;
        }
        LOG.infof("SqsEvaluationEventPublisher SendMessage queue=%s payload=%s", queueUrl, payload);
        messageSender.accept(queueUrl, payload);
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

    private static BiConsumer<String, String> defaultSender() {
        return (url, body) -> {
            try (SqsClient client = SqsClient.builder()
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .build()) {
                client.sendMessage(SendMessageRequest.builder()
                        .queueUrl(url)
                        .messageBody(body)
                        .build());
            }
        };
    }
}
