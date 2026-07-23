package com.fiap.feedbacks.lambda.report.infrastructure;

import com.fiap.feedbacks.lambda.report.application.port.ReportEmailSender;
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
 * Adapter SES HTML (SPEC-12.1 / SPEC-12.4).
 */
@ApplicationScoped
public class SesReportEmailSender implements ReportEmailSender {

    private static final Logger LOG = Logger.getLogger(SesReportEmailSender.class);

    private final SesClient sesClient;
    private final String fromEmail;

    @Inject
    public SesReportEmailSender(@ConfigProperty(name = "feedbacks.ses.from-email") String fromEmail) {
        this(SesClient.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build(), fromEmail);
    }

    SesReportEmailSender(SesClient sesClient, String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("SES_FROM_EMAIL / feedbacks.ses.from-email não configurado");
        }
        LOG.infof("SES SendEmail HTML from=%s to=%s subject=%s", fromEmail, to, subject);
        sesClient.sendEmail(SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                .build())
                        .build())
                .build());
    }
}
