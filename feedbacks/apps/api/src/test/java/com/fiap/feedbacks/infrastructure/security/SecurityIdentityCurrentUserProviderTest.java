package com.fiap.feedbacks.infrastructure.security;

import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.domain.exception.ForbiddenAccessException;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityIdentityCurrentUserProviderTest {

    @Mock
    SecurityIdentity securityIdentity;
    @Mock
    JsonWebToken jwt;

    @Test
    void returnsCurrentUserFromJwt() {
        UUID id = UUID.fromString("a1111111-1111-4111-8111-111111111111");
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(securityIdentity.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(securityIdentity.hasRole("ESTUDANTE")).thenReturn(true);

        var provider = new SecurityIdentityCurrentUserProvider(securityIdentity, jwt);

        assertThat(provider.getCurrentUserId()).isEqualTo(id);
        assertThat(provider.getCurrentRole()).isEqualTo(Papel.ESTUDANTE);
        assertThat(provider.hasRole(Papel.ESTUDANTE)).isTrue();
        assertThat(provider.hasRole(Papel.ADMINISTRADOR)).isFalse();
        assertThat(provider.getCurrentUser().papel()).isEqualTo(Papel.ESTUDANTE);
    }

    @Test
    void adminRoleFromSecurityIdentity() {
        UUID id = UUID.randomUUID();
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(securityIdentity.hasRole("ADMINISTRADOR")).thenReturn(true);

        var provider = new SecurityIdentityCurrentUserProvider(securityIdentity, jwt);
        assertThat(provider.getCurrentRole()).isEqualTo(Papel.ADMINISTRADOR);
    }

    @Test
    void roleFromClaimWhenGroupsMissing() {
        UUID id = UUID.randomUUID();
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(jwt.getSubject()).thenReturn(id.toString());
        when(securityIdentity.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(securityIdentity.hasRole("ESTUDANTE")).thenReturn(false);
        when(jwt.getClaim("role")).thenReturn("ESTUDANTE");

        var provider = new SecurityIdentityCurrentUserProvider(securityIdentity, jwt);
        assertThat(provider.getCurrentRole()).isEqualTo(Papel.ESTUDANTE);
    }

    @Test
    void anonymous_throwsForbidden() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        var provider = new SecurityIdentityCurrentUserProvider(securityIdentity, jwt);
        assertThatThrownBy(provider::getCurrentUser).isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void missingRole_throwsForbidden() {
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(jwt.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(securityIdentity.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(securityIdentity.hasRole("ESTUDANTE")).thenReturn(false);
        when(jwt.getClaim("role")).thenReturn(null);

        var provider = new SecurityIdentityCurrentUserProvider(securityIdentity, jwt);
        assertThatThrownBy(provider::getCurrentRole).isInstanceOf(ForbiddenAccessException.class);
    }
}
