package com.fiap.feedbacks.lambda.report.infrastructure;

import com.fiap.feedbacks.lambda.report.application.port.ReportEmailSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fake in-memory de e-mail HTML para testes.
 */
public class FakeReportEmailSender implements ReportEmailSender {

    public record SentHtmlEmail(String to, String subject, String htmlBody) {
    }

    private final List<SentHtmlEmail> sent = new ArrayList<>();
    private final AtomicBoolean failNext = new AtomicBoolean(false);

    @Override
    public void sendHtml(String to, String subject, String htmlBody) {
        if (failNext.compareAndSet(true, false)) {
            throw new IllegalStateException("SES fake failure");
        }
        sent.add(new SentHtmlEmail(to, subject, htmlBody));
    }

    public void failNextSend() {
        failNext.set(true);
    }

    public List<SentHtmlEmail> sentEmails() {
        return Collections.unmodifiableList(sent);
    }

    public void clear() {
        sent.clear();
        failNext.set(false);
    }
}
