package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "aula")
public class AulaEntity {

    @Id
    private UUID id;

    @Column(name = "curso_id", nullable = false)
    private UUID cursoId;

    @Column(nullable = false)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected AulaEntity() {
    }

    public AulaEntity(UUID id, UUID cursoId, String nome, String descricao, OffsetDateTime criadoEm) {
        this.id = id;
        this.cursoId = cursoId;
        this.nome = nome;
        this.descricao = descricao;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCursoId() {
        return cursoId;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
