package com.fiap.feedbacks.lambda.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.feedbacks.lambda.notification.domain.AlertPayloadParser;
import com.fiap.feedbacks.lambda.notification.infrastructure.FakeEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessAlertUseCaseTest {

    private FakeEmailSender emailSender;
    private ProcessAlertUseCase useCase;

    @BeforeEach
    void setUp() {
        emailSender = new FakeEmailSender();
        useCase = new ProcessAlertUseCase(
                new AlertPayloadParser(new ObjectMapper()),
                emailSender,
                "admin@demo.fiap");
    }

    @Test
    void spec101_payloadAltaDisparaSesUmaVez() throws IOException {
        useCase.process(fixture());

        assertThat(emailSender.sentEmails()).hasSize(1);
        var sent = emailSender.sentEmails().get(0);
        assertThat(sent.to()).isEqualTo("admin@demo.fiap");
        assertThat(sent.subject()).isEqualTo(ProcessAlertUseCase.EMAIL_SUBJECT);
        assertThat(sent.body())
                .contains("Aula rápida demais")
                .contains("ALTA")
                .contains("2026-07-22T14:30:00Z");
    }

    @Test
    void spec104_bodyInvalidoNaoChamaSes() {
        assertThatThrownBy(() -> useCase.process("{bad"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(emailSender.sentEmails()).isEmpty();
    }

    @Test
    void spec105_falhaSesPropaga() throws IOException {
        emailSender.failNextSend();
        assertThatThrownBy(() -> useCase.process(fixture()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SES");
    }

    @Test
    void adminEmailAusenteFalhaSemEnviar() throws IOException {
        useCase = new ProcessAlertUseCase(
                new AlertPayloadParser(new ObjectMapper()),
                emailSender,
                "  ");
        assertThatThrownBy(() -> useCase.process(fixture()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");
        assertThat(emailSender.sentEmails()).isEmpty();
    }

    private static String fixture() throws IOException {
        try (var in = ProcessAlertUseCaseTest.class.getClassLoader()
                .getResourceAsStream("fixtures/ad5-alerta-alta.json")) {
            assertThat(in).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
