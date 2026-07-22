package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.application.auth.port.UsuarioRepository;
import com.fiap.feedbacks.domain.auth.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class JpaUsuarioRepository implements UsuarioRepository {

    private final UsuarioJpaRepository usuarioJpaRepository;

    @Inject
    public JpaUsuarioRepository(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return usuarioJpaRepository.findByEmail(email)
                .map(entity -> new Usuario(
                        entity.getId(),
                        entity.getEmail(),
                        entity.getSenhaHash(),
                        entity.getPapel()
                ));
    }
}
