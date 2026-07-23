package com.fiap.feedbacks.lambda.report.infrastructure;

import com.fiap.feedbacks.lambda.report.application.port.ReportPdfStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Grava PDF no S3 com chave {@code relatorios/<periodo>/yyyy-MM-dd/<uuid>.pdf} (SPEC-12.2).
 */
@ApplicationScoped
public class S3ReportPdfStore implements ReportPdfStore {

    private static final Logger LOG = Logger.getLogger(S3ReportPdfStore.class);

    private final S3Client s3Client;
    private final String bucket;

    @Inject
    public S3ReportPdfStore(@ConfigProperty(name = "feedbacks.report.s3.bucket") String bucket) {
        this(S3Client.builder().httpClientBuilder(UrlConnectionHttpClient.builder()).build(), bucket);
    }

    S3ReportPdfStore(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String store(String objectKey, byte[] pdfBytes) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("REPORT_S3_BUCKET / feedbacks.report.s3.bucket não configurado");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey obrigatório");
        }
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("pdfBytes vazio");
        }
        LOG.infof("S3 PutObject bucket=%s key=%s bytes=%d", bucket, objectKey, pdfBytes.length);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(pdfBytes));
        return objectKey;
    }
}
