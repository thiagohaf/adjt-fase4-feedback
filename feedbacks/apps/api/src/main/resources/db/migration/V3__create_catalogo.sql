CREATE TABLE curso (
    id          UUID PRIMARY KEY,
    nome        VARCHAR(255) NOT NULL,
    descricao   VARCHAR(1000),
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE aula (
    id          UUID PRIMARY KEY,
    curso_id    UUID NOT NULL REFERENCES curso (id),
    nome        VARCHAR(255) NOT NULL,
    descricao   VARCHAR(1000),
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_aula_curso_id ON aula (curso_id);
