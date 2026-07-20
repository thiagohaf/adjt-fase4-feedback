package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "avaliacao")
public class AvaliacaoEntity {

    @Id
    private UUID id;

    @Column(name = "estudante_id", nullable = false)
    private UUID estudanteId;

    @Column(nullable = false)
    private String descricao;

    protected AvaliacaoEntity() {
    }

    public AvaliacaoEntity(UUID id, UUID estudanteId, String descricao) {
        this.id = id;
        this.estudanteId = estudanteId;
        this.descricao = descricao;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEstudanteId() {
        return estudanteId;
    }

    public String getDescricao() {
        return descricao;
    }
}
