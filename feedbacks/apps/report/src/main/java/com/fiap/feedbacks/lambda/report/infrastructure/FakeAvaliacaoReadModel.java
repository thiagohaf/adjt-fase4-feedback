package com.fiap.feedbacks.lambda.report.infrastructure;

import com.fiap.feedbacks.lambda.report.application.port.AvaliacaoReadModel;
import com.fiap.feedbacks.lambda.report.domain.AvaliacaoSnapshot;
import com.fiap.feedbacks.lambda.report.domain.ReportWindow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fake in-memory para testes (SPEC-12.5 / task 5.1).
 */
public class FakeAvaliacaoReadModel implements AvaliacaoReadModel {

    private final List<AvaliacaoSnapshot> store = new CopyOnWriteArrayList<>();

    public void add(AvaliacaoSnapshot snapshot) {
        store.add(snapshot);
    }

    public void addAll(List<AvaliacaoSnapshot> snapshots) {
        store.addAll(snapshots);
    }

    public void clear() {
        store.clear();
    }

    @Override
    public List<AvaliacaoSnapshot> findInWindow(ReportWindow window) {
        List<AvaliacaoSnapshot> matched = new ArrayList<>();
        for (AvaliacaoSnapshot s : store) {
            if (!s.ocorridoEm().isBefore(window.inicio()) && s.ocorridoEm().isBefore(window.fim())) {
                matched.add(s);
            }
        }
        return Collections.unmodifiableList(matched);
    }
}
