package com.fiap.feedbacks.infrastructure.security;

import com.fiap.feedbacks.application.auth.dto.AuthenticatedUser;
import com.fiap.feedbacks.application.auth.port.TokenService;
import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.domain.exception.InvalidTokenException;
import com.fiap.feedbacks.domain.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService implements TokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generate(AuthenticatedUser user) {
        var now = Instant.now();
        var expiration = now.plus(jwtProperties.expiration());

        return Jwts.builder()
                .subject(user.id().toString())
                .claim("role", user.papel().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            var userId = UUID.fromString(claims.getSubject());
            var role = Papel.valueOf(claims.get("role", String.class));

            return new AuthenticatedUser(userId, null, role);
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException();
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException ex) {
            throw new InvalidTokenException();
        }
    }

    @Override
    public long expiresInSeconds() {
        return jwtProperties.expiration().getSeconds();
    }
}
