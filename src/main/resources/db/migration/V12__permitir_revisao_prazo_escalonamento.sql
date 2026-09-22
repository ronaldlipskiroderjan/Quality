ALTER TABLE escalonamentos_nc
    ADD COLUMN prazo_original_em TIMESTAMP(6) WITH TIME ZONE;

UPDATE escalonamentos_nc
SET prazo_original_em = prazo_em
WHERE prazo_original_em IS NULL;

ALTER TABLE escalonamentos_nc
    ALTER COLUMN prazo_original_em SET NOT NULL;

ALTER TABLE escalonamentos_nc
    ADD COLUMN revisado_em TIMESTAMP(6) WITH TIME ZONE;

CREATE INDEX idx_escalonamento_responsavel_prazo
    ON escalonamentos_nc (responsavel_participacao_id, prazo_em);
