UPDATE participacao_plano_papeis
SET papel = CASE papel
    WHEN 'RESPONSAVEL_RESOLUCAO' THEN 'MEMBRO_EQUIPE_RESOLUCAO'
    WHEN 'RESPONSAVEL_N1' THEN 'SUPERIOR_N1'
    WHEN 'RESPONSAVEL_N2' THEN 'SUPERIOR_N2'
    ELSE papel
END;

DELETE FROM participacao_plano_papeis papel_repetido
WHERE EXISTS (
    SELECT 1
    FROM participacao_plano_papeis papel_preferido
    WHERE papel_preferido.participacao_id = papel_repetido.participacao_id
      AND CASE papel_preferido.papel
          WHEN 'RESPONSAVEL_QUALIDADE' THEN 1
          WHEN 'AUDITOR' THEN 2
          WHEN 'MEMBRO_EQUIPE_RESOLUCAO' THEN 3
          WHEN 'SUPERIOR_N1' THEN 4
          WHEN 'SUPERIOR_N2' THEN 5
          ELSE 99
      END < CASE papel_repetido.papel
          WHEN 'RESPONSAVEL_QUALIDADE' THEN 1
          WHEN 'AUDITOR' THEN 2
          WHEN 'MEMBRO_EQUIPE_RESOLUCAO' THEN 3
          WHEN 'SUPERIOR_N1' THEN 4
          WHEN 'SUPERIOR_N2' THEN 5
          ELSE 99
      END
);

ALTER TABLE participacao_plano_papeis
    ADD CONSTRAINT uk_participacao_papel_unico UNIQUE (participacao_id);

ALTER TABLE nao_conformidades
    ALTER COLUMN responsavel_participacao_id DROP NOT NULL;

CREATE TABLE encaminhamentos_nc (
    id UUID PRIMARY KEY,
    nao_conformidade_id UUID NOT NULL,
    chave_idempotencia VARCHAR(100) NOT NULL,
    total_destinatarios INTEGER NOT NULL,
    encaminhado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    versao_registro BIGINT NOT NULL,
    CONSTRAINT uk_encaminhamento_nc_idempotencia
        UNIQUE (nao_conformidade_id, chave_idempotencia),
    CONSTRAINT fk_encaminhamento_nc_nao_conformidade
        FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id)
);
