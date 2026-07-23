package com.fiap.feedbacks.lambda.report.application.port;

/**
 * Persistência do PDF no S3 (SPEC-12.2).
 */
public interface ReportPdfStore {

    /**
     * @return chave do objeto gravado
     */
    String store(String objectKey, byte[] pdfBytes);
}
