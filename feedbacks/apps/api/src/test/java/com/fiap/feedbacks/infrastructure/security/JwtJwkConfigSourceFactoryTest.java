package com.fiap.feedbacks.infrastructure.security;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtJwkConfigSourceFactoryTest {

    @Test
    void buildsJwkFromSecret() {
        String jwk = JwtJwkConfigSourceFactory.toSymmetricJwk("secret");
        assertThat(jwk).contains("\"kty\":\"oct\"");
        assertThat(jwk).contains("\"k\":");
    }

    @Test
    void emptySecret_returnsNoSources() {
        var context = mock(ConfigSourceContext.class);
        when(context.getValue("app.jwt.secret")).thenReturn(null);

        var factory = new JwtJwkConfigSourceFactory();
        assertThat(factory.getConfigSources(context)).isEmpty();
        assertThat(factory.getPriority()).isPresent();
    }

    @Test
    void blankSecret_returnsNoSources() {
        var context = mock(ConfigSourceContext.class);
        var value = mock(ConfigValue.class);
        when(value.getValue()).thenReturn("  ");
        when(context.getValue("app.jwt.secret")).thenReturn(value);

        assertThat(new JwtJwkConfigSourceFactory().getConfigSources(context)).isEmpty();
    }
}
