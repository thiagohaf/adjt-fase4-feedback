package com.fiap.feedbacks.lambda.notification.infrastructure;

import com.fiap.feedbacks.lambda.notification.application.port.EmailSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fake in-memory para testes unitários (SPEC-10.1 / 10.4).
 */
public class FakeEmailSender implements EmailSender {

    public record SentEmail(String to, String subject, String body) {
    }

    private final List<SentEmail> sent = new ArrayList<>();
    private final AtomicBoolean failNext = new AtomicBoolean(false);

    @Override
    public void send(String to, String subject, String body) {
        if (failNext.compareAndSet(true, false)) {
            throw new IllegalStateException("SES fake failure");
        }
        sent.add(new SentEmail(to, subject, body));
    }

    public void failNextSend() {
        failNext.set(true);
    }

    public List<SentEmail> sentEmails() {
        return Collections.unmodifiableList(sent);
    }

    public void clear() {
        sent.clear();
        failNext.set(false);
    }
}
