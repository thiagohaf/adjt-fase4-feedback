package com.fiap.feedbacks.lambda.report.application;

import com.fiap.feedbacks.lambda.report.domain.AvaliacaoSnapshot;
import com.fiap.feedbacks.lambda.report.domain.ReportAggregates;
import com.fiap.feedbacks.lambda.report.domain.ReportWindow;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gera PDF tabular leve (OpenPDF) com métricas e lista individual do período (D6).
 */
@ApplicationScoped
public class ReportPdfGenerator {

    private static final DateTimeFormatter DATA_ENVIO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ReportWindow.ZONE_SP);

    public byte[] generate(ReportAggregates aggregates) {
        return generate(aggregates, List.of());
    }

    public byte[] generate(ReportAggregates aggregates, List<AvaliacaoSnapshot> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph(
                    "Relatório " + aggregates.periodo().wireValue() + " — Plataforma de Feedbacks",
                    titleFont));
            document.add(new Paragraph(
                    "Período: " + aggregates.window().inicio() + " → " + aggregates.window().fim()
                            + " (America/Sao_Paulo, half-open)",
                    bodyFont));
            document.add(new Paragraph(" "));

            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(100);
            addRow(summary, "Média de notas", String.format(Locale.US, "%.2f", aggregates.mediaNota()), bodyFont);
            addRow(summary, "Quantidade total", String.valueOf(aggregates.total()), bodyFont);
            document.add(summary);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Quantidade por urgência", titleFont));
            PdfPTable urgTable = new PdfPTable(2);
            urgTable.setWidthPercentage(100);
            addRow(urgTable, "Urgência", "Qty", bodyFont);
            for (Map.Entry<String, Long> e : aggregates.qtyPorUrgencia().entrySet()) {
                addRow(urgTable, e.getKey(), String.valueOf(e.getValue()), bodyFont);
            }
            document.add(urgTable);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Quantidade por dia civil (SP)", titleFont));
            PdfPTable dayTable = new PdfPTable(2);
            dayTable.setWidthPercentage(100);
            addRow(dayTable, "Dia", "Qty", bodyFont);
            if (aggregates.qtyPorDia().isEmpty()) {
                addRow(dayTable, "—", "0", bodyFont);
            } else {
                for (Map.Entry<LocalDate, Long> e : aggregates.qtyPorDia().entrySet()) {
                    addRow(dayTable, e.getKey().toString(), String.valueOf(e.getValue()), bodyFont);
                }
            }
            document.add(dayTable);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Avaliações (Descrição / Urgência / Data de envio)", titleFont));
            PdfPTable items = new PdfPTable(3);
            items.setWidthPercentage(100);
            addCell(items, "Descrição", bodyFont);
            addCell(items, "Urgência", bodyFont);
            addCell(items, "Data de envio", bodyFont);
            List<AvaliacaoSnapshot> ordered = rows == null
                    ? List.of()
                    : rows.stream()
                            .sorted(Comparator.comparing(AvaliacaoSnapshot::ocorridoEm).reversed())
                            .toList();
            if (ordered.isEmpty()) {
                addCell(items, "Nenhuma avaliação no período", bodyFont);
                addCell(items, "", bodyFont);
                addCell(items, "", bodyFont);
            } else {
                for (AvaliacaoSnapshot row : ordered) {
                    addCell(items, row.descricao() == null ? "" : row.descricao(), bodyFont);
                    addCell(items, row.urgencia(), bodyFont);
                    addCell(items, DATA_ENVIO.format(row.ocorridoEm()), bodyFont);
                }
            }
            document.add(items);

            document.close();
            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Falha ao gerar PDF do relatório", ex);
        }
    }

    private static void addRow(PdfPTable table, String left, String right, Font font) {
        table.addCell(new PdfPCell(new Phrase(left, font)));
        table.addCell(new PdfPCell(new Phrase(right, font)));
    }

    private static void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new PdfPCell(new Phrase(text, font)));
    }
}
