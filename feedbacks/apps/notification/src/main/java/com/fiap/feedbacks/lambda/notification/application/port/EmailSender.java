package com.fiap.feedbacks.lambda.notification.application.port;

/**
 * Porta de envio de e-mail (adapter SES em produção; fake em testes).
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
