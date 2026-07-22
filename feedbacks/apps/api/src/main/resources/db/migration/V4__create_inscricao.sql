CREATE TABLE inscricao_curso (
    id            UUID PRIMARY KEY,
    estudante_id  UUID NOT NULL REFERENCES usuario (id),
    curso_id      UUID NOT NULL REFERENCES curso (id),
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inscricao_curso_estudante_curso UNIQUE (estudante_id, curso_id)
);

CREATE TABLE inscricao_aula (
    id            UUID PRIMARY KEY,
    estudante_id  UUID NOT NULL REFERENCES usuario (id),
    aula_id       UUID NOT NULL REFERENCES aula (id),
    curso_id      UUID NOT NULL REFERENCES curso (id),
    criado_em     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inscricao_aula_estudante_aula UNIQUE (estudante_id, aula_id)
);

CREATE INDEX idx_inscricao_curso_estudante ON inscricao_curso (estudante_id);
CREATE INDEX idx_inscricao_aula_estudante ON inscricao_aula (estudante_id);
CREATE INDEX idx_inscricao_aula_aula ON inscricao_aula (aula_id);
