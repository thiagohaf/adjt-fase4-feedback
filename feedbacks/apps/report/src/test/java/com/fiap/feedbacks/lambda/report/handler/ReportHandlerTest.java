package com.fiap.feedbacks.lambda.report.handler;

import com.fiap.feedbacks.lambda.report.application.GenerateReportUseCase;
import com.fiap.feedbacks.lambda.report.application.ReportPdfGenerator;
import com.fiap.feedbacks.lambda.report.domain.Periodo;
import com.fiap.feedbacks.lambda.report.infrastructure.FakeAvaliacaoReadModel;
import com.fiap.feedbacks.lambda.report.infrastructure.FakeReportEmailSender;
import com.fiap.feedbacks.lambda.report.infrastructure.FakeReportPdfStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportHandlerTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    private FakeReportEmailSender emailSender;
    private FakeReportPdfStore pdfStore;
    private ReportHandler handler;

    @BeforeEach
    void setUp() {
        FakeAvaliacaoReadModel readModel = new FakeAvaliacaoReadModel();
        emailSender = new FakeReportEmailSender();
        pdfStore = new FakeReportPdfStore();
        Instant fixed = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        GenerateReportUseCase useCase = new GenerateReportUseCase(
                readModel, new ReportPdfGenerator(), pdfStore, emailSender, "admin@example.test");
        handler = new ReportHandler(useCase, fixed);
    }

    @Test
    void periodoInvalidoFalha() {
        assertThatThrownBy(() -> handler.handleRequest(new ReportRequest("mensal"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodo");
        assertThat(emailSender.sentEmails()).isEmpty();
        assertThat(pdfStore.storedPdfs()).isEmpty();
    }

    @Test
    void nullPayloadFalha() {
        assertThatThrownBy(() -> handler.handleRequest(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void diarioESemanalMesmoDeployable() {
        Map<String, Object> diario = handler.handleRequest(new ReportRequest("diario"), null);
        Map<String, Object> semanal = handler.handleRequest(new ReportRequest("semanal"), null);

        assertThat(diario.get("periodo")).isEqualTo(Periodo.DIARIO.wireValue());
        assertThat(semanal.get("periodo")).isEqualTo(Periodo.SEMANAL.wireValue());
        assertThat(emailSender.sentEmails()).hasSize(2);
        assertThat(pdfStore.storedPdfs()).hasSize(2);
    }

    @Test
    void handlerPublicoUsaTriggerAgora() {
        FakeAvaliacaoReadModel readModel = new FakeAvaliacaoReadModel();
        GenerateReportUseCase useCase = new GenerateReportUseCase(
                readModel,
                new ReportPdfGenerator(),
                new FakeReportPdfStore(),
                new FakeReportEmailSender(),
                "admin@example.test");
        ReportHandler live = new ReportHandler(useCase);
        Map<String, Object> out = live.handleRequest(new ReportRequest("diario"), null);
        assertThat(out.get("periodo")).isEqualTo("diario");
        assertThat(out.get("s3ObjectKey").toString()).startsWith("relatorios/diario/");
    }
}
