package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inscricao_aula")
public class InscricaoAulaEntity {

    @Id
    private UUID id;

    @Column(name = "estudante_id", nullable = false)
    private UUID estudanteId;

    @Column(name = "aula_id", nullable = false)
    private UUID aulaId;

    @Column(name = "curso_id", nullable = false)
    private UUID cursoId;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected InscricaoAulaEntity() {
    }

    public InscricaoAulaEntity(
            UUID id,
            UUID estudanteId,
            UUID aulaId,
            UUID cursoId,
            OffsetDateTime criadoEm
    ) {
        this.id = id;
        this.estudanteId = estudanteId;
        this.aulaId = aulaId;
        this.cursoId = cursoId;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEstudanteId() {
        return estudanteId;
    }

    public UUID getAulaId() {
        return aulaId;
    }

    public UUID getCursoId() {
        return cursoId;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
