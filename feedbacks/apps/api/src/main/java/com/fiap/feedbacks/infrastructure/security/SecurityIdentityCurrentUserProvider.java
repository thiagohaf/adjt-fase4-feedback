package com.fiap.feedbacks.infrastructure.security;

import com.fiap.feedbacks.application.auth.dto.AuthenticatedUser;
import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.domain.exception.ForbiddenAccessException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@ApplicationScoped
public class SecurityIdentityCurrentUserProvider implements CurrentUserProvider {

    private final SecurityIdentity securityIdentity;
    private final JsonWebToken jwt;

    @Inject
    public SecurityIdentityCurrentUserProvider(SecurityIdentity securityIdentity, JsonWebToken jwt) {
        this.securityIdentity = securityIdentity;
        this.jwt = jwt;
    }

    @Override
    public AuthenticatedUser getCurrentUser() {
        requireAuthenticated();
        UUID id = UUID.fromString(jwt.getSubject());
        Papel papel = getCurrentRole();
        return new AuthenticatedUser(id, null, papel);
    }

    @Override
    public UUID getCurrentUserId() {
        return getCurrentUser().id();
    }

    @Override
    public Papel getCurrentRole() {
        requireAuthenticated();
        if (securityIdentity.hasRole(Papel.ADMINISTRADOR.name())) {
            return Papel.ADMINISTRADOR;
        }
        if (securityIdentity.hasRole(Papel.ESTUDANTE.name())) {
            return Papel.ESTUDANTE;
        }
        Object roleClaim = jwt.getClaim("role");
        if (roleClaim != null) {
            return Papel.valueOf(roleClaim.toString());
        }
        throw new ForbiddenAccessException();
    }

    @Override
    public boolean hasRole(Papel papel) {
        return getCurrentRole() == papel;
    }

    private void requireAuthenticated() {
        if (securityIdentity.isAnonymous() || jwt.getSubject() == null) {
            throw new ForbiddenAccessException();
        }
    }
}
