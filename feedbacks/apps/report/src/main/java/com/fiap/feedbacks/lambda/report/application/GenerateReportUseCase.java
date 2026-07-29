package com.fiap.feedbacks.lambda.report.application;

import com.fiap.feedbacks.lambda.report.application.port.AvaliacaoReadModel;
import com.fiap.feedbacks.lambda.report.application.port.ReportEmailSender;
import com.fiap.feedbacks.lambda.report.application.port.ReportPdfStore;
import com.fiap.feedbacks.lambda.report.domain.AvaliacaoSnapshot;
import com.fiap.feedbacks.lambda.report.domain.Periodo;
import com.fiap.feedbacks.lambda.report.domain.ReportAggregates;
import com.fiap.feedbacks.lambda.report.domain.ReportWindow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Janela → agrega → PDF → S3 → SES HTML (D4–D6). Sem writes de domínio (SPEC-11.3).
 * HTML/PDF incluem lista Descrição | Urgência | Data de envio além dos agregados.
 */
@ApplicationScoped
public class GenerateReportUseCase {

    private static final Logger LOG = Logger.getLogger(GenerateReportUseCase.class);
    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA_ENVIO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZONE_SP);

    private final AvaliacaoReadModel readModel;
    private final ReportPdfGenerator pdfGenerator;
    private final ReportPdfStore pdfStore;
    private final ReportEmailSender emailSender;
    private final String adminEmail;

    @Inject
    public GenerateReportUseCase(
            AvaliacaoReadModel readModel,
            ReportPdfGenerator pdfGenerator,
            ReportPdfStore pdfStore,
            ReportEmailSender emailSender,
            @ConfigProperty(name = "feedbacks.admin.email") String adminEmail) {
        this.readModel = readModel;
        this.pdfGenerator = pdfGenerator;
        this.pdfStore = pdfStore;
        this.emailSender = emailSender;
        this.adminEmail = adminEmail;
    }

    public ReportResult execute(Periodo periodo, Instant triggerInstant) {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException("ADMIN_EMAIL / feedbacks.admin.email não configurado");
        }
        Instant trigger = triggerInstant == null ? Instant.now() : triggerInstant;
        ReportWindow window = ReportWindow.forTrigger(periodo, trigger);
        List<AvaliacaoSnapshot> rows = readModel.findInWindow(window);
        ReportAggregates aggregates = ReportAggregates.from(window, rows);

        byte[] pdf = pdfGenerator.generate(aggregates, rows);
        String objectKey = buildObjectKey(periodo, window.keyDate());
        String storedKey = pdfStore.store(objectKey, pdf);

        String subject = "[Feedbacks] Relatório " + periodo.wireValue();
        String html = buildHtml(aggregates, rows);
        LOG.infof(
                "Relatório periodo=%s total=%d s3Key=%s to=%s",
                periodo.wireValue(),
                aggregates.total(),
                storedKey,
                adminEmail);
        emailSender.sendHtml(adminEmail, subject, html);

        return new ReportResult(aggregates, storedKey);
    }

    static String buildObjectKey(Periodo periodo, LocalDate keyDate) {
        return "relatorios/" + periodo.wireValue() + "/" + keyDate + "/" + java.util.UUID.randomUUID() + ".pdf";
    }

    static String buildHtml(ReportAggregates aggregates, List<AvaliacaoSnapshot> rows) {
        StringBuilder urgRows = new StringBuilder();
        for (Map.Entry<String, Long> e : aggregates.qtyPorUrgencia().entrySet()) {
            urgRows.append("<tr><td>")
                    .append(escapeHtml(e.getKey()))
                    .append("</td><td>")
                    .append(e.getValue())
                    .append("</td></tr>");
        }
        StringBuilder dayRows = new StringBuilder();
        if (aggregates.qtyPorDia().isEmpty()) {
            dayRows.append("<tr><td>—</td><td>0</td></tr>");
        } else {
            for (Map.Entry<LocalDate, Long> e : aggregates.qtyPorDia().entrySet()) {
                dayRows.append("<tr><td>")
                        .append(e.getKey())
                        .append("</td><td>")
                        .append(e.getValue())
                        .append("</td></tr>");
            }
        }
        StringBuilder itemRows = new StringBuilder();
        List<AvaliacaoSnapshot> ordered = rows == null
                ? List.of()
                : rows.stream()
                        .sorted(Comparator.comparing(AvaliacaoSnapshot::ocorridoEm).reversed())
                        .toList();
        if (ordered.isEmpty()) {
            itemRows.append("<tr><td colspan=\"3\">Nenhuma avaliação no período</td></tr>");
        } else {
            for (AvaliacaoSnapshot row : ordered) {
                itemRows.append("<tr><td>")
                        .append(escapeHtml(row.descricao()))
                        .append("</td><td>")
                        .append(escapeHtml(row.urgencia()))
                        .append("</td><td>")
                        .append(DATA_ENVIO.format(row.ocorridoEm()))
                        .append("</td></tr>");
            }
        }
        return String.format(
                Locale.US,
                """
                <html><body>
                <h2>Relatório %s — Plataforma de Feedbacks</h2>
                <p><strong>Tipo:</strong> %s</p>
                <p><strong>Período (America/Sao_Paulo):</strong> %s → %s (half-open)</p>
                <p><strong>Média de notas:</strong> %.2f</p>
                <p><strong>Quantidade total:</strong> %d</p>
                <h3>Por urgência</h3>
                <table border="1" cellpadding="4"><tr><th>Urgência</th><th>Qty</th></tr>%s</table>
                <h3>Por dia civil</h3>
                <table border="1" cellpadding="4"><tr><th>Dia</th><th>Qty</th></tr>%s</table>
                <h3>Avaliações (Descrição / Urgência / Data de envio)</h3>
                <table border="1" cellpadding="4"><tr><th>Descrição</th><th>Urgência</th><th>Data de envio</th></tr>%s</table>
                </body></html>
                """,
                aggregates.periodo().wireValue(),
                aggregates.periodo().wireValue(),
                aggregates.window().inicio(),
                aggregates.window().fim(),
                aggregates.mediaNota(),
                aggregates.total(),
                urgRows,
                dayRows,
                itemRows);
    }

    static String escapeHtml(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public record ReportResult(ReportAggregates aggregates, String s3ObjectKey) {
    }
}
