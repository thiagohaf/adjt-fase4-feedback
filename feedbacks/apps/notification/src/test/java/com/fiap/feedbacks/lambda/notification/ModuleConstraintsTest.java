package com.fiap.feedbacks.lambda.notification;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SPEC-10.2 / 10.6 — módulo notification sem JDBC/ORM e sem HTTP de negócio.
 */
class ModuleConstraintsTest {

    @Test
    void pomNaoDeclaraJdbcNemOrm() throws Exception {
        Path pom = Path.of("pom.xml");
        assertThat(Files.exists(pom)).isTrue();
        String xml = Files.readString(pom);
        assertThat(xml)
                .doesNotContain("quarkus-jdbc")
                .doesNotContain("quarkus-hibernate-orm")
                .doesNotContain("quarkus-flyway")
                .doesNotContain("quarkus-rest")
                .contains("quarkus-amazon-lambda");
    }

    @Test
    void hibernateNaoEstaNoClasspath() {
        assertThatThrownBy(() -> Class.forName("org.hibernate.Session"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
