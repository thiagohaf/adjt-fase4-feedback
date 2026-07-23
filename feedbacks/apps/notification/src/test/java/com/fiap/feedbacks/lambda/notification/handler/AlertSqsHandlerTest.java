package com.fiap.feedbacks.lambda.notification.handler;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.feedbacks.lambda.notification.application.ProcessAlertUseCase;
import com.fiap.feedbacks.lambda.notification.domain.AlertPayloadParser;
import com.fiap.feedbacks.lambda.notification.infrastructure.FakeEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertSqsHandlerTest {

    private FakeEmailSender emailSender;
    private AlertSqsHandler handler;

    @BeforeEach
    void setUp() {
        emailSender = new FakeEmailSender();
        var useCase = new ProcessAlertUseCase(
                new AlertPayloadParser(new ObjectMapper()),
                emailSender,
                "admin@demo.fiap");
        handler = new AlertSqsHandler(useCase);
    }

    @Test
    void processaRecordSqsComSucesso() throws IOException {
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
        msg.setMessageId("msg-1");
        msg.setBody(fixture());
        event.setRecords(List.of(msg));

        handler.handleRequest(event, null);

        assertThat(emailSender.sentEmails()).hasSize(1);
    }

    @Test
    void falhaParsePropagaParaRetry() {
        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
        msg.setMessageId("msg-bad");
        msg.setBody("not-json");
        event.setRecords(List.of(msg));

        assertThatThrownBy(() -> handler.handleRequest(event, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(emailSender.sentEmails()).isEmpty();
    }

    @Test
    void eventoVazioNaoFalha() {
        handler.handleRequest(null, null);
        handler.handleRequest(new SQSEvent(), null);
        assertThat(emailSender.sentEmails()).isEmpty();
    }

    private static String fixture() throws IOException {
        try (var in = AlertSqsHandlerTest.class.getClassLoader()
                .getResourceAsStream("fixtures/ad5-alerta-alta.json")) {
            assertThat(in).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
