package com.fiap.feedbacks.infrastructure.messaging;

import com.fiap.feedbacks.application.avaliacao.AvaliacaoAlertaEvent;
import com.fiap.feedbacks.domain.avaliacao.Urgencia;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EvaluationEventPublisherAdaptersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void kafkaPublisherSerializaPayloadAd5SemFalha() {
        var publisher = new KafkaEvaluationEventPublisher(objectMapper);
        assertThatCode(() -> publisher.publish(sampleEvent()))
                .doesNotThrowAnyException();
    }

    @Test
    void sqsPublisherNoOpQuandoQueueUrlAusente() {
        List<String> sent = new ArrayList<>();
        var publisher = new SqsEvaluationEventPublisher(
                objectMapper, "", (url, body) -> sent.add(body));
        assertThatCode(() -> publisher.publish(sampleEvent()))
                .doesNotThrowAnyException();
        assertThat(sent).isEmpty();
    }

    @Test
    void sqsPublisherEnviaQuandoQueueUrlConfigurada() throws IOException {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        var publisher = new SqsEvaluationEventPublisher(
                objectMapper,
                "https://sqs.us-east-1.amazonaws.com/123/alerta",
                (url, body) -> {
                    capturedUrl.set(url);
                    capturedBody.set(body);
                });

        AvaliacaoAlertaEvent event = fixtureEvent();
        publisher.publish(event);

        assertThat(capturedUrl.get()).contains("alerta");
        JsonNode node = objectMapper.readTree(capturedBody.get());
        JsonNode expected = objectMapper.readTree(fixtureJson());
        assertThat(node.get("avaliacaoId").asText()).isEqualTo(expected.get("avaliacaoId").asText());
        assertThat(node.get("descricao").asText()).isEqualTo(expected.get("descricao").asText());
        assertThat(node.get("urgencia").asText()).isEqualTo("ALTA");
        assertThat(node.get("ocorridoEm").asText()).isEqualTo(expected.get("ocorridoEm").asText());
        assertThat(node.get("aulaId").asText()).isEqualTo(expected.get("aulaId").asText());
        assertThat(node.get("cursoId").asText()).isEqualTo(expected.get("cursoId").asText());
    }

    private static AvaliacaoAlertaEvent sampleEvent() {
        return new AvaliacaoAlertaEvent(
                UUID.randomUUID(),
                "Aula rápida",
                Urgencia.ALTA,
                Instant.parse("2026-07-22T14:30:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }

    private static AvaliacaoAlertaEvent fixtureEvent() throws IOException {
        JsonNode n = new ObjectMapper().readTree(fixtureJson());
        return new AvaliacaoAlertaEvent(
                UUID.fromString(n.get("avaliacaoId").asText()),
                n.get("descricao").asText(),
                Urgencia.valueOf(n.get("urgencia").asText()),
                Instant.parse(n.get("ocorridoEm").asText()),
                UUID.fromString(n.get("aulaId").asText()),
                UUID.fromString(n.get("cursoId").asText())
        );
    }

    private static String fixtureJson() throws IOException {
        try (var in = EvaluationEventPublisherAdaptersTest.class.getClassLoader()
                .getResourceAsStream("fixtures/ad5-alerta-alta.json")) {
            assertThat(in).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
