package com.fiap.feedbacks.lambda.notification.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesEmailSenderTest {

    @Mock
    SesClient sesClient;

    @Test
    void enviaEmailComFromToSubjectBody() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("mid").build());

        var sender = new SesEmailSender(sesClient, "from@demo.fiap");
        sender.send("admin@demo.fiap", "[Feedbacks] Alerta ALTA", "corpo");

        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());
        SendEmailRequest req = captor.getValue();
        assertThat(req.source()).isEqualTo("from@demo.fiap");
        assertThat(req.destination().toAddresses()).containsExactly("admin@demo.fiap");
        assertThat(req.message().subject().data()).isEqualTo("[Feedbacks] Alerta ALTA");
        assertThat(req.message().body().text().data()).isEqualTo("corpo");
    }

    @Test
    void fromAusenteFalha() {
        var sender = new SesEmailSender(sesClient, " ");
        assertThatThrownBy(() -> sender.send("a@b.c", "s", "b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("from");
    }

    @Test
    void fromNullFalha() {
        var sender = new SesEmailSender(sesClient, null);
        assertThatThrownBy(() -> sender.send("a@b.c", "s", "b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("from");
    }
}
