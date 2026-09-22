CREATE TABLE configuracoes_classificacao (
    id UUID PRIMARY KEY,
    plano_id UUID NOT NULL,
    classificacao VARCHAR(20) NOT NULL,
    prazo_horas INTEGER NOT NULL,
    ativa BOOLEAN NOT NULL,
    CONSTRAINT uk_configuracao_classificacao_plano
        UNIQUE (plano_id, classificacao),
    CONSTRAINT fk_configuracao_classificacao_plano
        FOREIGN KEY (plano_id) REFERENCES planos (id) ON DELETE CASCADE
);

INSERT INTO configuracoes_classificacao (
    id,
    plano_id,
    classificacao,
    prazo_horas,
    ativa
)
SELECT
    gen_random_uuid(),
    plano.id,
    classificacao.nome,
    classificacao.prazo_horas,
    TRUE
FROM planos plano
CROSS JOIN (
    VALUES
        ('SIMPLES', 24),
        ('COMPLEXA', 48),
        ('SEVERA', 72),
        ('EXTREMA', 96)
) AS classificacao(nome, prazo_horas);

ALTER TABLE nao_conformidades
    DROP COLUMN prioridade;

CREATE TABLE auditoria_documentos_referencia (
    auditoria_id UUID NOT NULL,
    documento_id UUID NOT NULL,
    PRIMARY KEY (auditoria_id, documento_id),
    CONSTRAINT fk_auditoria_referencia_auditoria
        FOREIGN KEY (auditoria_id) REFERENCES auditorias (id) ON DELETE CASCADE,
    CONSTRAINT fk_auditoria_referencia_documento
        FOREIGN KEY (documento_id) REFERENCES documentos (id)
);

CREATE TABLE atividades_plano (
    id UUID PRIMARY KEY,
    plano_id UUID NOT NULL,
    autor_participacao_id UUID NOT NULL,
    acao VARCHAR(60) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    criado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_atividade_plano
        FOREIGN KEY (plano_id) REFERENCES planos (id) ON DELETE CASCADE,
    CONSTRAINT fk_atividade_autor
        FOREIGN KEY (autor_participacao_id) REFERENCES participacoes_plano (id)
);

CREATE INDEX idx_atividade_plano_criado
    ON atividades_plano (plano_id, criado_em DESC);
