ALTER TABLE auditorias
    ADD COLUMN conclusao_excepcional_autorizada_por_id UUID,
    ADD COLUMN conclusao_excepcional_autorizada_em TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN justificativa_conclusao_excepcional VARCHAR(2000);

ALTER TABLE auditorias
    ADD CONSTRAINT fk_auditoria_autorizacao_superior
    FOREIGN KEY (conclusao_excepcional_autorizada_por_id)
    REFERENCES participacoes_plano (id);

CREATE TABLE versoes_execucao_checklist (
    id UUID PRIMARY KEY,
    checklist_id UUID NOT NULL,
    autor_participacao_id UUID NOT NULL,
    numero INTEGER NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    observacao VARCHAR(500),
    criado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_versao_execucao_checklist_numero
        UNIQUE (checklist_id, numero),
    CONSTRAINT fk_versao_execucao_checklist
        FOREIGN KEY (checklist_id) REFERENCES checklists (id) ON DELETE CASCADE,
    CONSTRAINT fk_versao_execucao_autor
        FOREIGN KEY (autor_participacao_id) REFERENCES participacoes_plano (id)
);

CREATE TABLE itens_versao_execucao (
    id UUID PRIMARY KEY,
    versao_execucao_id UUID NOT NULL,
    ordem INTEGER NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    resultado VARCHAR(30),
    observacao VARCHAR(3000),
    nc_identificada_em TIMESTAMP(6) WITH TIME ZONE,
    responsavel_resolucao VARCHAR(150),
    classificacao_nc VARCHAR(20),
    acao_corretiva VARCHAR(3000),
    prazo_resolucao_em TIMESTAMP(6) WITH TIME ZONE,
    escalonado_em TIMESTAMP(6) WITH TIME ZONE,
    nc_concluida_em TIMESTAMP(6) WITH TIME ZONE,
    status_nc VARCHAR(30),
    CONSTRAINT fk_item_versao_execucao
        FOREIGN KEY (versao_execucao_id)
        REFERENCES versoes_execucao_checklist (id) ON DELETE CASCADE
);

CREATE INDEX idx_versao_execucao_checklist_data
    ON versoes_execucao_checklist (checklist_id, numero DESC);
