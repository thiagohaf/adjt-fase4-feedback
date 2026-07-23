package com.fiap.feedbacks.lambda.notification.infrastructure;

import com.fiap.feedbacks.lambda.notification.application.port.EmailSender;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

/**
 * Adapter SES (AWS SDK v2). Sem JDBC/ORM — só payload + config (SPEC-10.2).
 */
@ApplicationScoped
public class SesEmailSender implements EmailSender {

    private static final Logger LOG = Logger.getLogger(SesEmailSender.class);

    private final SesClient sesClient;
    private final String fromEmail;

    @Inject
    public SesEmailSender(@ConfigProperty(name = "feedbacks.ses.from-email") String fromEmail) {
        this(SesClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build(), fromEmail);
    }

    SesEmailSender(SesClient sesClient, String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("SES_FROM_EMAIL / feedbacks.ses.from-email não configurado");
        }
        LOG.infof("SES SendEmail from=%s to=%s subject=%s", fromEmail, to, subject);
        sesClient.sendEmail(SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .text(Content.builder().data(body).charset("UTF-8").build())
                                .build())
                        .build())
                .build());
    }
}
