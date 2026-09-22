ALTER TABLE itens_versao_execucao
    ADD COLUMN responsavel_participacao_id UUID;

ALTER TABLE itens_versao_execucao
    ADD CONSTRAINT fk_item_versao_responsavel
    FOREIGN KEY (responsavel_participacao_id)
    REFERENCES participacoes_plano (id);
