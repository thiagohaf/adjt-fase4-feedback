package com.fiap.feedbacks.application.auth;

import com.fiap.feedbacks.application.auth.dto.LoginCommand;
import com.fiap.feedbacks.application.auth.port.TokenService;
import com.fiap.feedbacks.application.auth.port.UsuarioRepository;
import com.fiap.feedbacks.domain.auth.Papel;
import com.fiap.feedbacks.domain.auth.Usuario;
import com.fiap.feedbacks.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private LoginUseCase loginUseCase;

    private static final UUID USER_ID = UUID.fromString("a1111111-1111-4111-8111-111111111111");

    @BeforeEach
    void setUp() {
        loginUseCase = new LoginUseCase(usuarioRepository, tokenService, passwordEncoder);
    }

    @Test
    void shouldLoginWithValidCredentials() {
        var usuario = new Usuario(USER_ID, "estudante@demo.fiap", "hash", Papel.ESTUDANTE);
        when(usuarioRepository.findByEmail("estudante@demo.fiap")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(tokenService.generate(org.mockito.ArgumentMatchers.any())).thenReturn("jwt-token");
        when(tokenService.expiresInSeconds()).thenReturn(86400L);

        var result = loginUseCase.execute(new LoginCommand("  ESTUDANTE@demo.fiap  ", "senha123"));

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.expiresInSeconds()).isEqualTo(86400L);
    }

    @Test
    void shouldRejectUnknownEmail() {
        when(usuarioRepository.findByEmail("naoexiste@demo.fiap")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("naoexiste@demo.fiap", "qualquer")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenService, never()).generate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectWrongPassword() {
        var usuario = new Usuario(USER_ID, "estudante@demo.fiap", "hash", Papel.ESTUDANTE);
        when(usuarioRepository.findByEmail("estudante@demo.fiap")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> loginUseCase.execute(new LoginCommand("estudante@demo.fiap", "errada")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenService, never()).generate(org.mockito.ArgumentMatchers.any());
    }
}
