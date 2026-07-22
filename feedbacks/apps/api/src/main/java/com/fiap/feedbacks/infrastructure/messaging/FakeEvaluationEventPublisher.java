package com.fiap.feedbacks.infrastructure.messaging;

import com.fiap.feedbacks.application.avaliacao.AvaliacaoAlertaEvent;
import com.fiap.feedbacks.application.avaliacao.port.EvaluationEventPublisher;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fake in-memory (%test) — registra publishes e pode simular falha (SPEC-10prep).
 */
@ApplicationScoped
@IfBuildProfile("test")
public class FakeEvaluationEventPublisher implements EvaluationEventPublisher {

    private final List<AvaliacaoAlertaEvent> published = new CopyOnWriteArrayList<>();
    private final AtomicBoolean failNext = new AtomicBoolean(false);

    @Override
    public void publish(AvaliacaoAlertaEvent event) {
        if (failNext.compareAndSet(true, false)) {
            throw new IllegalStateException("Falha simulada de publish");
        }
        published.add(event);
    }

    public List<AvaliacaoAlertaEvent> publishedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(published));
    }

    public void clear() {
        published.clear();
        failNext.set(false);
    }

    public void failNextPublish() {
        failNext.set(true);
    }
}
