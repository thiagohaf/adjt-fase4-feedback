package com.fiap.feedbacks.infrastructure.messaging;

import com.fiap.feedbacks.application.avaliacao.AvaliacaoAlertaEvent;
import com.fiap.feedbacks.domain.avaliacao.Urgencia;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

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
    void sqsPublisherSerializaPayloadAd5SemFalha() {
        var publisher = new SqsEvaluationEventPublisher(objectMapper);
        assertThatCode(() -> publisher.publish(sampleEvent()))
                .doesNotThrowAnyException();
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
}
