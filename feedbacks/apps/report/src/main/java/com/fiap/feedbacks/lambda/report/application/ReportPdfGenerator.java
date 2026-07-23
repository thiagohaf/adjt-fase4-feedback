package com.fiap.feedbacks.lambda.report.application;

import com.fiap.feedbacks.lambda.report.domain.ReportAggregates;
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
import java.util.Locale;
import java.util.Map;

/**
 * Gera PDF tabular leve (OpenPDF) com métricas do período (D6).
 */
@ApplicationScoped
public class ReportPdfGenerator {

    public byte[] generate(ReportAggregates aggregates) {
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
}
