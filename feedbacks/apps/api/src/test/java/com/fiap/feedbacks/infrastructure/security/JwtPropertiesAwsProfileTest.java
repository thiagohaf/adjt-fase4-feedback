package com.fiap.feedbacks.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesAwsProfileTest {

    @Test
    @DisplayName("SPEC-NFR-1 — profile %aws sem default de secret")
    void specNfr1_awsSecretSemDefault() throws IOException {
        Path props = Path.of("src/main/resources/application.properties");
        String content = Files.readString(props);

        assertThat(content).contains("%aws.app.jwt.secret=${JWT_SECRET}");
        assertThat(content).doesNotContain("%aws.app.jwt.secret=${JWT_SECRET:");
        // local pode ter default de dev
        assertThat(content).containsPattern("%local\\.app\\.jwt\\.secret=\\$\\{JWT_SECRET:");
    }
}
