package com.fiap.feedbacks.lambda.report.application.port;

/**
 * Porta de e-mail HTML do relatório (SPEC-12.1).
 */
public interface ReportEmailSender {

    void sendHtml(String to, String subject, String htmlBody);
}
