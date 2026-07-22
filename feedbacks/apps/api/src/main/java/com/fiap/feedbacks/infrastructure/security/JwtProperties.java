package com.fiap.feedbacks.infrastructure.security;

import io.smallrye.config.ConfigMapping;

import java.time.Duration;

@ConfigMapping(prefix = "app.jwt")
public interface JwtProperties {

    String secret();

    Duration expiration();
}
