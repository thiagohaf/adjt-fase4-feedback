package com.fiap.feedbacks.infrastructure.security;

import com.fiap.feedbacks.application.auth.dto.AuthenticatedUser;
import com.fiap.feedbacks.application.auth.port.TokenService;
import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.domain.exception.InvalidTokenException;
import com.fiap.feedbacks.domain.exception.TokenExpiredException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class SmallRyeJwtTokenService implements TokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private final JwtConsumer jwtConsumer;

    @Inject
    public SmallRyeJwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] secretBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        this.secretKey = new HmacKey(secretBytes);
        this.jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setVerificationKey(this.secretKey)
                .setSkipDefaultAudienceValidation()
                .build();
    }

    @Override
    public String generate(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.expiration());

        return Jwt.claims()
                .subject(user.id().toString())
                .claim("role", user.papel().name())
                .issuedAt(now)
                .expiresAt(expiration)
                .jws()
                .sign(secretKey);
    }

    @Override
    public AuthenticatedUser parse(String token) {
        try {
            JwtClaims claims = jwtConsumer.processToClaims(token);
            UUID userId = UUID.fromString(claims.getSubject());
            String role = claims.getStringClaimValue("role");
            return new AuthenticatedUser(userId, null, Papel.valueOf(role));
        } catch (InvalidJwtException ex) {
            if (isExpired(ex)) {
                throw new TokenExpiredException();
            }
            throw new InvalidTokenException();
        } catch (MalformedClaimException | IllegalArgumentException | NullPointerException ex) {
            throw new InvalidTokenException();
        }
    }

    @Override
    public long expiresInSeconds() {
        return jwtProperties.expiration().toSeconds();
    }

    private static boolean isExpired(InvalidJwtException ex) {
        String message = ex.getMessage();
        if (message != null && message.toLowerCase().contains("expir")) {
            return true;
        }
        return ex.getErrorDetails().stream()
                .anyMatch(detail -> {
                    String text = detail.toString().toLowerCase();
                    return text.contains("expir") || text.contains("the jwt is no longer valid");
                });
    }
}
