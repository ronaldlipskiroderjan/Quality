ALTER TABLE auditorias
    ALTER COLUMN checklist_id DROP NOT NULL;

INSERT INTO auditorias (
    id,
    artefato_id,
    checklist_id,
    auditor_participacao_id,
    data_inicio,
    conformes,
    nao_conformes,
    nao_aplicaveis,
    status,
    versao_registro
)
SELECT
    gen_random_uuid(),
    artefato.id,
    NULL,
    artefato.auditor_participacao_id,
    artefato.criado_em,
    0,
    0,
    0,
    'EM_PREPARACAO',
    0
FROM artefatos artefato
WHERE NOT EXISTS (
    SELECT 1
    FROM auditorias auditoria
    WHERE auditoria.artefato_id = artefato.id
);

ALTER TABLE checklists
    ADD COLUMN auditoria_id UUID;

UPDATE checklists checklist
SET auditoria_id = (
    SELECT auditoria.id
    FROM auditorias auditoria
    WHERE auditoria.artefato_id = checklist.artefato_id
    FETCH FIRST 1 ROW ONLY
);

ALTER TABLE checklists
    ALTER COLUMN auditoria_id SET NOT NULL;

ALTER TABLE checklists
    ADD CONSTRAINT fk_checklist_auditoria
    FOREIGN KEY (auditoria_id) REFERENCES auditorias (id) ON DELETE CASCADE;

ALTER TABLE checklists
    DROP CONSTRAINT uk_checklist_artefato_versao;

ALTER TABLE checklists
    DROP CONSTRAINT fk_checklist_artefato;

ALTER TABLE checklists
    DROP COLUMN artefato_id;

ALTER TABLE auditorias
    DROP CONSTRAINT fk_auditoria_checklist;

ALTER TABLE auditorias
    DROP COLUMN checklist_id;

ALTER TABLE auditorias
    ADD CONSTRAINT uk_auditoria_artefato UNIQUE (artefato_id);

ALTER TABLE checklists
    ADD CONSTRAINT uk_checklist_auditoria_titulo_versao
    UNIQUE (auditoria_id, titulo, versao);

CREATE INDEX idx_checklist_auditoria
    ON checklists (auditoria_id, atualizado_em DESC);
