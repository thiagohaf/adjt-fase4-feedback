package com.fiap.feedbacks.lambda.report.infrastructure;

import com.fiap.feedbacks.lambda.report.application.port.AvaliacaoReadModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;

/**
 * JDBC quando {@code feedbacks.report.jdbc.enabled=true}; senão janela vazia (demo sem RDS).
 */
@ApplicationScoped
public class AvaliacaoReadModelProducer {

    @Inject
    @ConfigProperty(name = "feedbacks.report.jdbc.enabled", defaultValue = "false")
    boolean jdbcEnabled;

    @Inject
    Instance<DataSource> dataSource;

    @Produces
    @ApplicationScoped
    AvaliacaoReadModel avaliacaoReadModel() {
        if (jdbcEnabled) {
            if (!dataSource.isResolvable()) {
                throw new IllegalStateException(
                        "feedbacks.report.jdbc.enabled=true mas DataSource não disponível");
            }
            return new JdbcAvaliacaoReadModel(dataSource.get());
        }
        return new EmptyAvaliacaoReadModel();
    }
}
