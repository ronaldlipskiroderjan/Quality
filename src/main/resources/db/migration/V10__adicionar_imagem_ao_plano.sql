ALTER TABLE planos
    ADD COLUMN tem_imagem BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE imagens_plano (
    plano_id UUID PRIMARY KEY,
    tipo_conteudo VARCHAR(50) NOT NULL,
    conteudo BYTEA NOT NULL,
    CONSTRAINT fk_imagem_plano
        FOREIGN KEY (plano_id) REFERENCES planos (id) ON DELETE CASCADE
);
