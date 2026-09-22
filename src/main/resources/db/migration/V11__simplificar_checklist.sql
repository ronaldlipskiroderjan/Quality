ALTER TABLE checklists
    DROP CONSTRAINT uk_checklist_auditoria_titulo_versao;

ALTER TABLE checklists
    ADD CONSTRAINT uk_checklist_auditoria_versao UNIQUE (auditoria_id, versao);

ALTER TABLE checklists
    DROP COLUMN titulo,
    DROP COLUMN descricao;
