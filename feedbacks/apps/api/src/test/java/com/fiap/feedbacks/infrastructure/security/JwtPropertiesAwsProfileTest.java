package com.fiap.feedbacks.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesAwsProfileTest {

    @Test
    @DisplayName("SPEC-NFR-1 — Segredo JWT fora do código (profile aws)")
    void specNfr1_awsProfileRequiresEnvSecret() {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-aws.yml"));
        var properties = factory.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("app.jwt.secret")).isEqualTo("${JWT_SECRET}");
        assertThat(properties.getProperty("app.jwt.secret")).doesNotContain("dev-only");
    }
}
