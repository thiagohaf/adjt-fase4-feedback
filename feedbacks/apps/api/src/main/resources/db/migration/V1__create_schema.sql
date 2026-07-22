CREATE TABLE usuario (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL,
    papel       VARCHAR(20)  NOT NULL,
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuario_papel CHECK (papel IN ('ESTUDANTE', 'ADMINISTRADOR'))
);

CREATE TABLE avaliacao (
    id            UUID PRIMARY KEY,
    estudante_id  UUID NOT NULL,
    descricao     VARCHAR(500) NOT NULL
);
