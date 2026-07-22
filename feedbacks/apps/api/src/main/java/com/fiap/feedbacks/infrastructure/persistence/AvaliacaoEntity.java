package com.fiap.feedbacks.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "avaliacao")
public class AvaliacaoEntity {

    @Id
    private UUID id;

    @Column(name = "estudante_id", nullable = false)
    private UUID estudanteId;

    @Column(name = "aula_id", nullable = false)
    private UUID aulaId;

    @Column(name = "curso_id", nullable = false)
    private UUID cursoId;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(nullable = false)
    private short nota;

    @Column(nullable = false, length = 10)
    private String urgencia;

    @Column(name = "ocorrido_em", nullable = false)
    private OffsetDateTime ocorridoEm;

    protected AvaliacaoEntity() {
    }

    public AvaliacaoEntity(
            UUID id,
            UUID estudanteId,
            UUID aulaId,
            UUID cursoId,
            String descricao,
            short nota,
            String urgencia,
            OffsetDateTime ocorridoEm
    ) {
        this.id = id;
        this.estudanteId = estudanteId;
        this.aulaId = aulaId;
        this.cursoId = cursoId;
        this.descricao = descricao;
        this.nota = nota;
        this.urgencia = urgencia;
        this.ocorridoEm = ocorridoEm;
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

    public String getDescricao() {
        return descricao;
    }

    public short getNota() {
        return nota;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
