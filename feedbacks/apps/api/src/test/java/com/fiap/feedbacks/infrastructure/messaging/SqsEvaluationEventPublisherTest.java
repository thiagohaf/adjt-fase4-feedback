package com.fiap.feedbacks.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.feedbacks.application.avaliacao.AvaliacaoAlertaEvent;
import com.fiap.feedbacks.domain.avaliacao.Urgencia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsEvaluationEventPublisherTest {

    @Mock
    ObjectMapper objectMapper;

    @Test
    void publishNoOpQuandoQueueUrlVazia() {
        List<String> sent = new ArrayList<>();
        var pub = new SqsEvaluationEventPublisher(new ObjectMapper(), "  ", (url, body) -> sent.add(body));
        pub.publish(sampleEvent());
        assertThat(sent).isEmpty();
    }

    @Test
    void publishEnviaQuandoQueueConfigurada() {
        AtomicReference<String> capturedUrl = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        var pub = new SqsEvaluationEventPublisher(
                new ObjectMapper(),
                "https://sqs.example/queue",
                (url, body) -> {
                    capturedUrl.set(url);
                    capturedBody.set(body);
                });
        pub.publish(sampleEvent());
        assertThat(capturedUrl.get()).isEqualTo("https://sqs.example/queue");
        assertThat(capturedBody.get()).contains("ALTA").contains("avaliacaoId");
    }

    @Test
    void publishAceitaQueueUrlNullComoVazia() {
        List<String> sent = new ArrayList<>();
        var pub = new SqsEvaluationEventPublisher(new ObjectMapper(), null, (url, body) -> sent.add(body));
        pub.publish(sampleEvent());
        assertThat(sent).isEmpty();
    }

    @Test
    void toJsonFalhaPropagaIllegalState() throws Exception {
        when(objectMapper.createObjectNode()).thenThrow(new RuntimeException("boom"));
        var pub = new SqsEvaluationEventPublisher(objectMapper, "https://q", (u, b) -> {
        });
        assertThatThrownBy(() -> pub.publish(sampleEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serializar");
    }

    private static AvaliacaoAlertaEvent sampleEvent() {
        return new AvaliacaoAlertaEvent(
                UUID.randomUUID(),
                "desc",
                Urgencia.ALTA,
                Instant.parse("2026-07-22T14:30:00Z"),
                UUID.randomUUID(),
                UUID.randomUUID());
    }
}
