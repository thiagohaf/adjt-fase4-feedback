package com.fiap.feedbacks.infrastructure.security;

import com.fiap.feedbacks.application.auth.dto.AuthenticatedUser;
import com.fiap.feedbacks.application.auth.port.CurrentUserProvider;
import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.domain.exception.ForbiddenAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SpringCurrentUserProvider implements CurrentUserProvider {

    @Override
    public AuthenticatedUser getCurrentUser() {
        var authentication = requireAuthentication();
        return (AuthenticatedUser) authentication.getPrincipal();
    }

    @Override
    public UUID getCurrentUserId() {
        return getCurrentUser().id();
    }

    @Override
    public Papel getCurrentRole() {
        return getCurrentUser().papel();
    }

    @Override
    public boolean hasRole(Papel papel) {
        return getCurrentRole() == papel;
    }

    private Authentication requireAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser)) {
            throw new ForbiddenAccessException();
        }
        return authentication;
    }
}
