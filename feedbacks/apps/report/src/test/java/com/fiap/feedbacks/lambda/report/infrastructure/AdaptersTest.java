package com.fiap.feedbacks.lambda.report.infrastructure;

import com.fiap.feedbacks.lambda.report.domain.AvaliacaoSnapshot;
import com.fiap.feedbacks.lambda.report.domain.Periodo;
import com.fiap.feedbacks.lambda.report.domain.ReportWindow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptersTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Mock
    DataSource dataSource;
    @Mock
    Connection connection;
    @Mock
    PreparedStatement preparedStatement;
    @Mock
    ResultSet resultSet;
    @Mock
    SesClient sesClient;
    @Mock
    S3Client s3Client;

    @Test
    void jdbcReadModelFiltraPorOcorridoEm() throws Exception {
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        ReportWindow window = ReportWindow.forTrigger(Periodo.DIARIO, trigger);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getShort("nota")).thenReturn((short) 7);
        when(resultSet.getString("descricao")).thenReturn("Aula ok");
        when(resultSet.getString("urgencia")).thenReturn("MEDIA");
        when(resultSet.getTimestamp("ocorrido_em"))
                .thenReturn(Timestamp.from(LocalDate.of(2026, 7, 21).atTime(10, 0).atZone(SP).toInstant()));

        JdbcAvaliacaoReadModel readModel = new JdbcAvaliacaoReadModel(dataSource);
        var rows = readModel.findInWindow(window);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).nota()).isEqualTo((short) 7);
        assertThat(rows.get(0).descricao()).isEqualTo("Aula ok");
        verify(preparedStatement).setTimestamp(1, Timestamp.from(window.inicio()));
        verify(preparedStatement).setTimestamp(2, Timestamp.from(window.fim()));
    }

    @Test
    void sesEnviaHtml() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("m1").build());
        SesReportEmailSender sender = new SesReportEmailSender(sesClient, "from@example.test");
        sender.sendHtml("to@example.test", "subj", "<b>hi</b>");
        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sesExigeFrom() {
        SesReportEmailSender blank = new SesReportEmailSender(sesClient, " ");
        assertThatThrownBy(() -> blank.sendHtml("a@b.c", "s", "<p>x</p>"))
                .isInstanceOf(IllegalStateException.class);
        SesReportEmailSender nil = new SesReportEmailSender(sesClient, null);
        assertThatThrownBy(() -> nil.sendHtml("a@b.c", "s", "<p>x</p>"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void s3StoreGravaPdf() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        S3ReportPdfStore store = new S3ReportPdfStore(s3Client, "bucket-relatorios");
        String key = store.store("relatorios/diario/2026-07-21/x.pdf", new byte[] {1, 2, 3});
        assertThat(key).isEqualTo("relatorios/diario/2026-07-21/x.pdf");
    }

    @Test
    void s3ExigeBucket() {
        S3ReportPdfStore blank = new S3ReportPdfStore(s3Client, "");
        assertThatThrownBy(() -> blank.store("k.pdf", new byte[] {1}))
                .isInstanceOf(IllegalStateException.class);
        S3ReportPdfStore nil = new S3ReportPdfStore(s3Client, null);
        assertThatThrownBy(() -> nil.store("k.pdf", new byte[] {1}))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void s3RejeitaKeyOuBytesInvalidos() {
        S3ReportPdfStore store = new S3ReportPdfStore(s3Client, "bucket");
        assertThatThrownBy(() -> store.store(" ", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.store("k.pdf", new byte[] {}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.store("k.pdf", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jdbcPropagaSqlException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("boom"));
        JdbcAvaliacaoReadModel readModel = new JdbcAvaliacaoReadModel(dataSource);
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        assertThatThrownBy(() -> readModel.findInWindow(ReportWindow.forTrigger(Periodo.DIARIO, trigger)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    void fakeReadModelRespeitaJanela() {
        FakeAvaliacaoReadModel fake = new FakeAvaliacaoReadModel();
        Instant trigger = ZonedDateTime.of(2026, 7, 22, 8, 0, 0, 0, SP).toInstant();
        ReportWindow window = ReportWindow.forTrigger(Periodo.DIARIO, trigger);
        fake.add(new AvaliacaoSnapshot(
                "in", (short) 5, "ALTA", LocalDate.of(2026, 7, 21).atTime(1, 0).atZone(SP).toInstant()));
        fake.add(new AvaliacaoSnapshot(
                "out", (short) 1, "BAIXA", LocalDate.of(2026, 7, 22).atTime(0, 0).atZone(SP).toInstant()));
        assertThat(fake.findInWindow(window)).hasSize(1);
    }
}
