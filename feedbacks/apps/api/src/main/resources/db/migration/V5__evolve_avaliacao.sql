-- Evolui avaliacao (V1) para FR-7+ / AD-16. Não altera V1–V4.
-- Remove linhas demo de V2 (sem aula_id/nota) antes dos NOT NULL — banco de teste é recriado pelo Flyway.

DELETE FROM avaliacao;

ALTER TABLE avaliacao
    ADD COLUMN aula_id UUID NOT NULL REFERENCES aula (id),
    ADD COLUMN curso_id UUID NOT NULL REFERENCES curso (id),
    ADD COLUMN nota SMALLINT NOT NULL,
    ADD COLUMN urgencia VARCHAR(10) NOT NULL,
    ADD COLUMN ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL;

ALTER TABLE avaliacao
    ADD CONSTRAINT chk_avaliacao_nota CHECK (nota BETWEEN 0 AND 10),
    ADD CONSTRAINT chk_avaliacao_urgencia CHECK (urgencia IN ('ALTA', 'MEDIA', 'BAIXA')),
    ADD CONSTRAINT uq_avaliacao_estudante_aula UNIQUE (estudante_id, aula_id);

CREATE INDEX idx_avaliacao_ocorrido_em ON avaliacao (ocorrido_em);
CREATE INDEX idx_avaliacao_aula ON avaliacao (aula_id);
