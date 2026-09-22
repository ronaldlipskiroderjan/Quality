UPDATE participacao_plano_papeis
SET papel = 'RESPONSAVEL_QUALIDADE'
WHERE papel = 'PROPRIETARIO'
  AND NOT EXISTS (
      SELECT 1
      FROM participacao_plano_papeis existente
      WHERE existente.participacao_id = participacao_plano_papeis.participacao_id
        AND existente.papel = 'RESPONSAVEL_QUALIDADE'
  );

ALTER TABLE checklists
    ADD COLUMN versao_anterior_id UUID;

ALTER TABLE checklists
    ADD CONSTRAINT fk_checklist_versao_anterior
    FOREIGN KEY (versao_anterior_id) REFERENCES checklists (id);
