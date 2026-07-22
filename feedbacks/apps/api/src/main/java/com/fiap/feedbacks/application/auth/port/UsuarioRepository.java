package com.fiap.feedbacks.application.auth.port;

import com.fiap.feedbacks.domain.auth.Usuario;

import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> findByEmail(String email);
}
