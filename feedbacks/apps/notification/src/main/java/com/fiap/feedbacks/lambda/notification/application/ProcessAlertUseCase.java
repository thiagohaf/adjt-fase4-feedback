package com.fiap.feedbacks.lambda.notification.application;

import com.fiap.feedbacks.lambda.notification.application.port.EmailSender;
import com.fiap.feedbacks.lambda.notification.domain.AlertPayload;
import com.fiap.feedbacks.lambda.notification.domain.AlertPayloadParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Parse AD-5 → montar e-mail → {@link EmailSender#send}.
 */
@ApplicationScoped
public class ProcessAlertUseCase {

    public static final String EMAIL_SUBJECT = "[Feedbacks] Alerta ALTA";

    private static final Logger LOG = Logger.getLogger(ProcessAlertUseCase.class);

    private final AlertPayloadParser parser;
    private final EmailSender emailSender;
    private final String adminEmail;

    @Inject
    public ProcessAlertUseCase(
            AlertPayloadParser parser,
            EmailSender emailSender,
            @ConfigProperty(name = "feedbacks.admin.email") String adminEmail) {
        this.parser = parser;
        this.emailSender = emailSender;
        this.adminEmail = adminEmail;
    }

    public void process(String messageBody) {
        AlertPayload payload = parser.parse(messageBody);
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException("ADMIN_EMAIL / feedbacks.admin.email não configurado");
        }
        String body = buildBody(payload);
        LOG.infof(
                "Enviando alerta SES avaliacaoId=%s urgencia=%s to=%s",
                payload.avaliacaoId(),
                payload.urgencia(),
                adminEmail);
        emailSender.send(adminEmail, EMAIL_SUBJECT, body);
    }

    static String buildBody(AlertPayload payload) {
        return """
                Alerta de urgência — Plataforma de Feedbacks

                Descrição: %s
                Urgência: %s
                Data (ocorridoEm): %s

                avaliacaoId: %s
                aulaId: %s
                cursoId: %s
                """.formatted(
                payload.descricao(),
                payload.urgencia(),
                payload.ocorridoEm(),
                payload.avaliacaoId(),
                payload.aulaId(),
                payload.cursoId());
    }
}
