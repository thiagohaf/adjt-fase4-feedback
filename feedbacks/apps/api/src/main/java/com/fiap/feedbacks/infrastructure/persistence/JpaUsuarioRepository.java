package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.application.auth.port.UsuarioRepository;
import com.fiap.feedbacks.domain.auth.Usuario;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaUsuarioRepository implements UsuarioRepository {

    private final UsuarioJpaRepository usuarioJpaRepository;

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
