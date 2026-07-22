package com.fiap.feedbacks.support;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;

import java.nio.charset.StandardCharsets;

public final class JwtClaimsReader {

    private JwtClaimsReader() {
    }

    public static JwtClaims readClaims(String token, String secret) {
        try {
            return new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(60)
                    .setRequireSubject()
                    .setVerificationKey(new HmacKey(secret.getBytes(StandardCharsets.UTF_8)))
                    .setSkipDefaultAudienceValidation()
                    .build()
                    .processToClaims(token);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read JWT claims", e);
        }
    }
}
