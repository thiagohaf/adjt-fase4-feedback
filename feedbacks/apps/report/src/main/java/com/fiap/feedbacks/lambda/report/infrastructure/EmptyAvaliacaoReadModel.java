package com.fiap.feedbacks.lambda.report.infrastructure;

import com.fiap.feedbacks.lambda.report.application.port.AvaliacaoReadModel;
import com.fiap.feedbacks.lambda.report.domain.AvaliacaoSnapshot;
import com.fiap.feedbacks.lambda.report.domain.ReportWindow;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Demo sem RDS: janela vazia (SPEC-12.5 zeros).
 * Instanciado via {@link AvaliacaoReadModelProducer} quando JDBC está desabilitado.
 */
public class EmptyAvaliacaoReadModel implements AvaliacaoReadModel {

    private static final Logger LOG = Logger.getLogger(EmptyAvaliacaoReadModel.class);

    @Override
    public List<AvaliacaoSnapshot> findInWindow(ReportWindow window) {
        LOG.infof(
                "Read model vazio (sem JDBC/RDS) window [%s, %s) → 0 linhas",
                window.inicio(),
                window.fim());
        return List.of();
    }
}
