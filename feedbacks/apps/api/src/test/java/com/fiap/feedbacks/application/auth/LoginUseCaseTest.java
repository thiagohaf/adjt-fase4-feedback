package com.fiap.feedbacks.application.auth;

import com.fiap.feedbacks.application.auth.dto.AuthenticatedUser;
import com.fiap.feedbacks.application.auth.dto.LoginCommand;
import com.fiap.feedbacks.application.auth.port.PasswordEncoder;
import com.fiap.feedbacks.application.auth.port.TokenService;
import com.fiap.feedbacks.application.auth.port.UsuarioRepository;
import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.domain.auth.Usuario;
import com.fiap.feedbacks.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    TokenService tokenService;
    @Mock
    PasswordEncoder passwordEncoder;

    LoginUseCase loginUseCase;

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(usuarioRepository, tokenService, passwordEncoder);
    }

    @Test
    void credentialsOk_returnsToken() {
        var usuario = new Usuario(USER_ID, "estudante@demo.fiap", "hash", Papel.ESTUDANTE);
        when(usuarioRepository.findByEmail("estudante@demo.fiap")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(tokenService.generate(any())).thenReturn("jwt-token");
        when(tokenService.expiresInSeconds()).thenReturn(86400L);

        var result = loginUseCase.execute(new LoginCommand("estudante@demo.fiap", "senha123"));

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.expiresInSeconds()).isEqualTo(86400L);

        ArgumentCaptor<AuthenticatedUser> captor = ArgumentCaptor.forClass(AuthenticatedUser.class);
        verify(tokenService).generate(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(USER_ID);
        assertThat(captor.getValue().papel()).isEqualTo(Papel.ESTUDANTE);
    }

    @Test
    void emailInexistente_throwsInvalidCredentials() {
        when(usuarioRepository.findByEmail("naoexiste@demo.fiap")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("naoexiste@demo.fiap", "senha123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void senhaErrada_throwsInvalidCredentials() {
        var usuario = new Usuario(USER_ID, "estudante@demo.fiap", "hash", Papel.ESTUDANTE);
        when(usuarioRepository.findByEmail("estudante@demo.fiap")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("estudante@demo.fiap", "errada")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void normalizaEmail_lowercaseTrim() {
        var usuario = new Usuario(USER_ID, "estudante@demo.fiap", "hash", Papel.ESTUDANTE);
        when(usuarioRepository.findByEmail("estudante@demo.fiap")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(eq("senha123"), eq("hash"))).thenReturn(true);
        when(tokenService.generate(any())).thenReturn("jwt");
        when(tokenService.expiresInSeconds()).thenReturn(3600L);

        loginUseCase.execute(new LoginCommand("  Estudante@Demo.Fiap  ", "senha123"));

        verify(usuarioRepository).findByEmail("estudante@demo.fiap");
    }
}
