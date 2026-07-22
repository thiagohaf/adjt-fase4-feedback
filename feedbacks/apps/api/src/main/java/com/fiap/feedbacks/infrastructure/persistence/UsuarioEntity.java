package com.fiap.feedbacks.infrastructure.persistence;

import com.fiap.feedbacks.domain.auth.Papel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class UsuarioEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Papel papel;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected UsuarioEntity() {
    }

    public UsuarioEntity(UUID id, String email, String senhaHash, Papel papel, OffsetDateTime criadoEm) {
        this.id = id;
        this.email = email;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
