-- Um plano e um artefato são raízes de agregados. Quando sua exclusão é
-- confirmada, os registros que só existem dentro deles devem ser removidos
-- pela mesma operação no banco, sem deixar históricos órfãos.

ALTER TABLE participacoes_plano
    DROP CONSTRAINT fk_participacao_plano_plano;

ALTER TABLE participacoes_plano
    ADD CONSTRAINT fk_participacao_plano_plano
    FOREIGN KEY (plano_id) REFERENCES planos (id) ON DELETE CASCADE;

ALTER TABLE participacao_plano_papeis
    DROP CONSTRAINT fk_participacao_papeis_participacao;

ALTER TABLE participacao_plano_papeis
    ADD CONSTRAINT fk_participacao_papeis_participacao
    FOREIGN KEY (participacao_id) REFERENCES participacoes_plano (id)
    ON DELETE CASCADE;

ALTER TABLE documentos
    DROP CONSTRAINT fk_documento_plano;

ALTER TABLE documentos
    ADD CONSTRAINT fk_documento_plano
    FOREIGN KEY (plano_id) REFERENCES planos (id) ON DELETE CASCADE;

ALTER TABLE artefatos
    DROP CONSTRAINT fk_artefato_documento;

ALTER TABLE artefatos
    ADD CONSTRAINT fk_artefato_documento
    FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE;

ALTER TABLE auditorias
    DROP CONSTRAINT fk_auditoria_artefato;

ALTER TABLE auditorias
    ADD CONSTRAINT fk_auditoria_artefato
    FOREIGN KEY (artefato_id) REFERENCES artefatos (id) ON DELETE CASCADE;

ALTER TABLE auditoria_documentos_referencia
    DROP CONSTRAINT fk_auditoria_referencia_documento;

ALTER TABLE auditoria_documentos_referencia
    ADD CONSTRAINT fk_auditoria_referencia_documento
    FOREIGN KEY (documento_id) REFERENCES documentos (id) ON DELETE CASCADE;

ALTER TABLE itens_checklist
    DROP CONSTRAINT fk_item_checklist_checklist;

ALTER TABLE itens_checklist
    ADD CONSTRAINT fk_item_checklist_checklist
    FOREIGN KEY (checklist_id) REFERENCES checklists (id) ON DELETE CASCADE;

ALTER TABLE respostas_auditoria
    DROP CONSTRAINT fk_resposta_auditoria_auditoria;

ALTER TABLE respostas_auditoria
    ADD CONSTRAINT fk_resposta_auditoria_auditoria
    FOREIGN KEY (auditoria_id) REFERENCES auditorias (id) ON DELETE CASCADE;

ALTER TABLE respostas_auditoria
    DROP CONSTRAINT fk_resposta_auditoria_item;

ALTER TABLE respostas_auditoria
    ADD CONSTRAINT fk_resposta_auditoria_item
    FOREIGN KEY (item_checklist_id) REFERENCES itens_checklist (id)
    ON DELETE CASCADE;

ALTER TABLE nao_conformidades
    DROP CONSTRAINT fk_nc_resposta;

ALTER TABLE nao_conformidades
    ADD CONSTRAINT fk_nc_resposta
    FOREIGN KEY (resposta_id) REFERENCES respostas_auditoria (id)
    ON DELETE CASCADE;

ALTER TABLE comunicacoes_nc
    DROP CONSTRAINT fk_comunicacao_nc_nao_conformidade;

ALTER TABLE comunicacoes_nc
    ADD CONSTRAINT fk_comunicacao_nc_nao_conformidade
    FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id)
    ON DELETE CASCADE;

ALTER TABLE resolucoes_nc
    DROP CONSTRAINT fk_resolucao_nc_nao_conformidade;

ALTER TABLE resolucoes_nc
    ADD CONSTRAINT fk_resolucao_nc_nao_conformidade
    FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id)
    ON DELETE CASCADE;

ALTER TABLE escalonamentos_nc
    DROP CONSTRAINT fk_escalonamento_nc_nao_conformidade;

ALTER TABLE escalonamentos_nc
    ADD CONSTRAINT fk_escalonamento_nc_nao_conformidade
    FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id)
    ON DELETE CASCADE;

ALTER TABLE notificacoes
    DROP CONSTRAINT fk_notificacao_nao_conformidade;

ALTER TABLE notificacoes
    ADD CONSTRAINT fk_notificacao_nao_conformidade
    FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id)
    ON DELETE CASCADE;

ALTER TABLE encaminhamentos_nc
    DROP CONSTRAINT fk_encaminhamento_nc_nao_conformidade;

ALTER TABLE encaminhamentos_nc
    ADD CONSTRAINT fk_encaminhamento_nc_nao_conformidade
    FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id)
    ON DELETE CASCADE;
