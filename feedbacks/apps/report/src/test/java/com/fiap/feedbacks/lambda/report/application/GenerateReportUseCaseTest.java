package com.fiap.feedbacks.lambda.report.application;

import com.fiap.feedbacks.lambda.report.domain.AvaliacaoSnapshot;
import com.fiap.feedbacks.lambda.report.domain.Periodo;
import com.fiap.feedbacks.lambda.report.infrastructure.FakeAvaliacaoReadModel;
import com.fiap.feedbacks.lambda.report.infrastructure.FakeReportEmailSender;
import com.fiap.feedbacks.lambda.report.infrastructure.FakeReportPdfStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SPEC-12.1 / 12.2 / 12.5 — use case com fakes (sem mutação de domínio).
 */
class GenerateReportUseCaseTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final String ADMIN = "admin@example.test";

    private FakeAvaliacaoReadModel readModel;
    private FakeReportEmailSender emailSender;
    private FakeReportPdfStore pdfStore;
    private GenerateReportUseCase useCase;

    @BeforeEach
    void setUp() {
        readModel = new FakeAvaliacaoReadModel();
        emailSender = new FakeReportEmailSender();
        pdfStore = new FakeReportPdfStore();
        useCase = new GenerateReportUseCase(
                readModel, new ReportPdfGenerator(), pdfStore, emailSender, ADMIN);
    }

    @Test
    void periodoVazioAindaEnviaEmailEPdf() {
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();

        GenerateReportUseCase.ReportResult result = useCase.execute(Periodo.DIARIO, trigger);

        assertThat(result.aggregates().total()).isZero();
        assertThat(emailSender.sentEmails()).hasSize(1);
        assertThat(emailSender.sentEmails().get(0).to()).isEqualTo(ADMIN);
        assertThat(emailSender.sentEmails().get(0).htmlBody())
                .contains("diario")
                .contains("Quantidade total:")
                .contains("<strong>Média de notas:</strong> 0.00");
        assertThat(pdfStore.storedPdfs()).hasSize(1);
        assertThat(pdfStore.storedPdfs().get(0).objectKey())
                .matches("relatorios/diario/2026-07-21/[0-9a-f\\-]+\\.pdf");
        assertThat(pdfStore.storedPdfs().get(0).bytes()[0]).isEqualTo((byte) '%'); // %PDF
    }

    @Test
    void semanalAgregaEEntregaComTipoNoHtml() {
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        Instant inWindow = LocalDate.of(2026, 7, 20).atTime(15, 0).atZone(SP).toInstant();
        Instant outWindow = LocalDate.of(2026, 7, 14).atTime(12, 0).atZone(SP).toInstant();
        readModel.add(new AvaliacaoSnapshot((short) 9, "ALTA", inWindow));
        readModel.add(new AvaliacaoSnapshot((short) 3, "BAIXA", outWindow));

        GenerateReportUseCase.ReportResult result = useCase.execute(Periodo.SEMANAL, trigger);

        assertThat(result.aggregates().total()).isEqualTo(1);
        assertThat(result.aggregates().mediaNota()).isEqualTo(9.0);
        assertThat(emailSender.sentEmails().get(0).subject()).contains("semanal");
        assertThat(emailSender.sentEmails().get(0).htmlBody()).contains("ALTA");
        assertThat(pdfStore.storedPdfs().get(0).objectKey()).startsWith("relatorios/semanal/");
    }

    @Test
    void adminEmailObrigatorio() {
        GenerateReportUseCase brokenBlank = new GenerateReportUseCase(
                readModel, new ReportPdfGenerator(), pdfStore, emailSender, " ");
        assertThatThrownBy(() -> brokenBlank.execute(Periodo.DIARIO, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAIL");

        GenerateReportUseCase brokenNull = new GenerateReportUseCase(
                readModel, new ReportPdfGenerator(), pdfStore, emailSender, null);
        assertThatThrownBy(() -> brokenNull.execute(Periodo.DIARIO, Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void triggerNuloUsaAgora() {
        GenerateReportUseCase.ReportResult result = useCase.execute(Periodo.DIARIO, null);
        assertThat(result.aggregates()).isNotNull();
        assertThat(emailSender.sentEmails()).hasSize(1);
        assertThat(pdfStore.storedPdfs()).hasSize(1);
    }
}
