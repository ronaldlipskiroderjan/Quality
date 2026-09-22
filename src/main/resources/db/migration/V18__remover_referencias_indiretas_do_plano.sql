-- Participações pertencem ao plano. Seus vínculos operacionais também devem
-- desaparecer, ou ser desassociados quando o campo é opcional, durante a
-- exclusão definitiva confirmada pelo usuário.

ALTER TABLE artefatos
    DROP CONSTRAINT fk_artefato_auditor;

ALTER TABLE artefatos
    ADD CONSTRAINT fk_artefato_auditor
    FOREIGN KEY (auditor_participacao_id) REFERENCES participacoes_plano (id)
    ON DELETE CASCADE;

ALTER TABLE auditorias
    DROP CONSTRAINT fk_auditoria_auditor;

ALTER TABLE auditorias
    ADD CONSTRAINT fk_auditoria_auditor
    FOREIGN KEY (auditor_participacao_id) REFERENCES participacoes_plano (id)
    ON DELETE CASCADE;

ALTER TABLE auditorias
    DROP CONSTRAINT fk_auditoria_autorizacao_superior;

ALTER TABLE auditorias
    ADD CONSTRAINT fk_auditoria_autorizacao_superior
    FOREIGN KEY (conclusao_excepcional_autorizada_por_id)
    REFERENCES participacoes_plano (id) ON DELETE SET NULL;

ALTER TABLE nao_conformidades
    DROP CONSTRAINT fk_nc_responsavel;

ALTER TABLE nao_conformidades
    ADD CONSTRAINT fk_nc_responsavel
    FOREIGN KEY (responsavel_participacao_id)
    REFERENCES participacoes_plano (id) ON DELETE SET NULL;

ALTER TABLE resolucoes_nc
    DROP CONSTRAINT fk_resolucao_nc_responsavel;

ALTER TABLE resolucoes_nc
    ADD CONSTRAINT fk_resolucao_nc_responsavel
    FOREIGN KEY (responsavel_participacao_id)
    REFERENCES participacoes_plano (id) ON DELETE CASCADE;

ALTER TABLE resolucoes_nc
    DROP CONSTRAINT fk_resolucao_nc_auditor;

ALTER TABLE resolucoes_nc
    ADD CONSTRAINT fk_resolucao_nc_auditor
    FOREIGN KEY (auditor_participacao_id)
    REFERENCES participacoes_plano (id) ON DELETE SET NULL;

ALTER TABLE escalonamentos_nc
    DROP CONSTRAINT fk_escalonamento_nc_responsavel;

ALTER TABLE escalonamentos_nc
    ADD CONSTRAINT fk_escalonamento_nc_responsavel
    FOREIGN KEY (responsavel_participacao_id)
    REFERENCES participacoes_plano (id) ON DELETE CASCADE;

ALTER TABLE escalonamentos_nc
    DROP CONSTRAINT fk_escalonamento_nc_auditor;

ALTER TABLE escalonamentos_nc
    ADD CONSTRAINT fk_escalonamento_nc_auditor
    FOREIGN KEY (auditor_participacao_id)
    REFERENCES participacoes_plano (id) ON DELETE CASCADE;

ALTER TABLE notificacoes
    DROP CONSTRAINT fk_notificacao_destinatario;

ALTER TABLE notificacoes
    ADD CONSTRAINT fk_notificacao_destinatario
    FOREIGN KEY (destinatario_participacao_id)
    REFERENCES participacoes_plano (id) ON DELETE CASCADE;

ALTER TABLE atividades_plano
    DROP CONSTRAINT fk_atividade_autor;

ALTER TABLE atividades_plano
    ADD CONSTRAINT fk_atividade_autor
    FOREIGN KEY (autor_participacao_id)
    REFERENCES participacoes_plano (id) ON DELETE CASCADE;

ALTER TABLE versoes_execucao_checklist
    DROP CONSTRAINT fk_versao_execucao_autor;

ALTER TABLE versoes_execucao_checklist
    ADD CONSTRAINT fk_versao_execucao_autor
    FOREIGN KEY (autor_participacao_id)
    REFERENCES participacoes_plano (id) ON DELETE CASCADE;

ALTER TABLE itens_versao_execucao
    DROP CONSTRAINT fk_item_versao_responsavel;

ALTER TABLE itens_versao_execucao
    ADD CONSTRAINT fk_item_versao_responsavel
    FOREIGN KEY (responsavel_participacao_id)
    REFERENCES participacoes_plano (id) ON DELETE SET NULL;

ALTER TABLE checklists
    DROP CONSTRAINT fk_checklist_versao_anterior;

ALTER TABLE checklists
    ADD CONSTRAINT fk_checklist_versao_anterior
    FOREIGN KEY (versao_anterior_id) REFERENCES checklists (id)
    ON DELETE SET NULL;
