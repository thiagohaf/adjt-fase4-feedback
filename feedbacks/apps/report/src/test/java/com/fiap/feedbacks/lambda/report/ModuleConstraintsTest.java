package com.fiap.feedbacks.lambda.report;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SPEC-11.3 — módulo report distinto, read-only (JDBC ok; sem Flyway/REST de domínio).
 */
class ModuleConstraintsTest {

    @Test
    void pomDeclaraLambdaJdbcSemFlywayNemRest() throws Exception {
        Path pom = Path.of("pom.xml");
        assertThat(Files.exists(pom)).isTrue();
        String xml = Files.readString(pom);
        assertThat(xml)
                .contains("quarkus-amazon-lambda")
                .contains("quarkus-jdbc-postgresql")
                .contains("feedbacks-report")
                .doesNotContain("quarkus-flyway")
                .doesNotContain("quarkus-rest")
                .doesNotContain("quarkus-hibernate-orm");
    }

    @Test
    void hibernateNaoEstaNoClasspath() {
        assertThatThrownBy(() -> Class.forName("org.hibernate.Session"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void pacoteBaseEhReport() {
        assertThat(com.fiap.feedbacks.lambda.report.domain.Periodo.class.getPackageName())
                .startsWith("com.fiap.feedbacks.lambda.report");
    }
}
