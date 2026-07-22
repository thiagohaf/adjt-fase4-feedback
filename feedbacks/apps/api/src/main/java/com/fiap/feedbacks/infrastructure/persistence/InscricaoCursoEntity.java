package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inscricao_curso")
public class InscricaoCursoEntity {

    @Id
    private UUID id;

    @Column(name = "estudante_id", nullable = false)
    private UUID estudanteId;

    @Column(name = "curso_id", nullable = false)
    private UUID cursoId;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected InscricaoCursoEntity() {
    }

    public InscricaoCursoEntity(UUID id, UUID estudanteId, UUID cursoId, OffsetDateTime criadoEm) {
        this.id = id;
        this.estudanteId = estudanteId;
        this.cursoId = cursoId;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEstudanteId() {
        return estudanteId;
    }

    public UUID getCursoId() {
        return cursoId;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
