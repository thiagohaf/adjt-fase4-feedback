package com.fiap.feedbacks.lambda.report.infrastructure;

import com.fiap.feedbacks.lambda.report.application.port.AvaliacaoReadModel;
import com.fiap.feedbacks.lambda.report.domain.AvaliacaoSnapshot;
import com.fiap.feedbacks.lambda.report.domain.ReportWindow;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only JDBC na tabela {@code avaliacao} (AD-16 / SPEC-11.3) — sem Flyway neste módulo.
 * Instanciado via {@link AvaliacaoReadModelProducer} quando JDBC está habilitado.
 */
public class JdbcAvaliacaoReadModel implements AvaliacaoReadModel {

    private static final Logger LOG = Logger.getLogger(JdbcAvaliacaoReadModel.class);

    private static final String SQL = """
            SELECT descricao, nota, urgencia, ocorrido_em
              FROM avaliacao
             WHERE ocorrido_em >= ? AND ocorrido_em < ?
            """;

    private final DataSource dataSource;

    public JdbcAvaliacaoReadModel(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<AvaliacaoSnapshot> findInWindow(ReportWindow window) {
        List<AvaliacaoSnapshot> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setTimestamp(1, Timestamp.from(window.inicio()));
            ps.setTimestamp(2, Timestamp.from(window.fim()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AvaliacaoSnapshot(
                            rs.getString("descricao"),
                            rs.getShort("nota"),
                            rs.getString("urgencia"),
                            rs.getTimestamp("ocorrido_em").toInstant()));
                }
            }
            LOG.infof("Read model window [%s, %s) → %d linhas", window.inicio(), window.fim(), rows.size());
            return rows;
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao ler avaliações (read-only)", ex);
        }
    }
}
