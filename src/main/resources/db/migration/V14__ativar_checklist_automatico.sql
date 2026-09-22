UPDATE checklists
SET status = 'PUBLICADO',
    atualizado_em = CURRENT_TIMESTAMP
WHERE status = 'RASCUNHO';

UPDATE checklists checklist
SET status = 'PUBLICADO',
    atualizado_em = CURRENT_TIMESTAMP
WHERE checklist.id IN (
    SELECT candidata.id
    FROM (
        SELECT
            versao.id,
            ROW_NUMBER() OVER (
                PARTITION BY versao.auditoria_id
                ORDER BY versao.atualizado_em DESC, versao.criado_em DESC
            ) AS ordem
        FROM checklists versao
        WHERE NOT EXISTS (
            SELECT 1
            FROM checklists ativa
            WHERE ativa.auditoria_id = versao.auditoria_id
              AND ativa.status <> 'ARQUIVADO'
        )
    ) candidata
    WHERE candidata.ordem = 1
);

INSERT INTO checklists (
    id,
    auditoria_id,
    versao_anterior_id,
    versao,
    status,
    criado_em,
    atualizado_em,
    versao_registro
)
SELECT
    gen_random_uuid(),
    auditoria.id,
    NULL,
    '1.0',
    'PUBLICADO',
    auditoria.data_inicio,
    CURRENT_TIMESTAMP,
    0
FROM auditorias auditoria
WHERE NOT EXISTS (
    SELECT 1
    FROM checklists checklist
    WHERE checklist.auditoria_id = auditoria.id
      AND checklist.status <> 'ARQUIVADO'
);

INSERT INTO itens_checklist (
    id,
    checklist_id,
    ordem,
    pergunta,
    origem,
    criado_em
)
SELECT
    gen_random_uuid(),
    checklist.id,
    1,
    '',
    'MANUAL',
    checklist.criado_em
FROM checklists checklist
WHERE checklist.status = 'PUBLICADO'
  AND NOT EXISTS (
      SELECT 1
      FROM itens_checklist item
      WHERE item.checklist_id = checklist.id
  );

UPDATE auditorias
SET status = 'EM_ANDAMENTO'
WHERE status = 'EM_PREPARACAO';

UPDATE artefatos
SET status = 'EM_ANDAMENTO'
WHERE status = 'EM_PREPARACAO'
  AND EXISTS (
      SELECT 1
      FROM auditorias auditoria
      WHERE auditoria.artefato_id = artefatos.id
        AND auditoria.status = 'EM_ANDAMENTO'
  );
