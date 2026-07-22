package com.fiap.feedbacks.application.auth;

import com.fiap.feedbacks.application.auth.dto.AuthenticatedUser;
import com.fiap.feedbacks.application.auth.dto.LoginCommand;
import com.fiap.feedbacks.application.auth.dto.LoginResult;
import com.fiap.feedbacks.application.auth.port.PasswordEncoder;
import com.fiap.feedbacks.application.auth.port.TokenService;
import com.fiap.feedbacks.application.auth.port.UsuarioRepository;
import com.fiap.feedbacks.domain.exception.InvalidCredentialsException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LoginUseCase {

    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Inject
    public LoginUseCase(
            UsuarioRepository usuarioRepository,
            TokenService tokenService,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResult execute(LoginCommand command) {
        var normalizedEmail = command.email().trim().toLowerCase();
        var usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.password(), usuario.senhaHash())) {
            throw new InvalidCredentialsException();
        }

        var authenticatedUser = new AuthenticatedUser(
                usuario.id(),
                usuario.email(),
                usuario.papel()
        );
        var accessToken = tokenService.generate(authenticatedUser);
        var expiresInSeconds = tokenService.expiresInSeconds();

        return new LoginResult(accessToken, expiresInSeconds);
    }
}
