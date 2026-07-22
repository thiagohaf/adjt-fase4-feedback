package com.fiap.feedbacks.infrastructure.security;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.common.MapBackedConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Bootstrap: deriva {@code smallrye.jwt.verify.secretkey} (JWK oct) a partir de
 * {@code app.jwt.secret} — fonte única jwtSecret (design §9.2).
 */
public class JwtJwkConfigSourceFactory implements ConfigSourceFactory {

    private static final String SECRET_KEY = "app.jwt.secret";
    private static final String VERIFY_SECRET_KEY = "smallrye.jwt.verify.secretkey";

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        ConfigValue secretValue = context.getValue(SECRET_KEY);
        if (secretValue == null || secretValue.getValue() == null || secretValue.getValue().isBlank()) {
            return Collections.emptyList();
        }

        String jwk = toSymmetricJwk(secretValue.getValue());
        Map<String, String> properties = new HashMap<>();
        properties.put(VERIFY_SECRET_KEY, jwk);

        return Collections.singletonList(
                new MapBackedConfigSource("JwtJwkFromAppSecret", properties, 275) {
                }
        );
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(275);
    }

    static String toSymmetricJwk(String secret) {
        String k = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(secret.getBytes(StandardCharsets.UTF_8));
        return "{\"kty\":\"oct\",\"k\":\"" + k + "\"}";
    }
}
