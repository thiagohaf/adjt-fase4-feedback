package com.fiap.feedbacks.lambda.report.infrastructure;

import com.fiap.feedbacks.lambda.report.application.port.ReportPdfStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fake store de PDF para testes (SPEC-12.2 / SPEC-12.5).
 */
public class FakeReportPdfStore implements ReportPdfStore {

    public record StoredPdf(String objectKey, byte[] bytes) {
    }

    private final List<StoredPdf> stored = new ArrayList<>();
    private final AtomicBoolean failNext = new AtomicBoolean(false);

    @Override
    public String store(String objectKey, byte[] pdfBytes) {
        if (failNext.compareAndSet(true, false)) {
            throw new IllegalStateException("S3 fake failure");
        }
        stored.add(new StoredPdf(objectKey, pdfBytes.clone()));
        return objectKey;
    }

    public void failNextStore() {
        failNext.set(true);
    }

    public List<StoredPdf> storedPdfs() {
        return Collections.unmodifiableList(stored);
    }

    public void clear() {
        stored.clear();
        failNext.set(false);
    }
}
