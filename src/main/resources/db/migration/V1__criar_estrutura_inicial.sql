CREATE TABLE roles (
    id UUID PRIMARY KEY,
    nome VARCHAR(255)
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL,
    criado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    tipo_imagem VARCHAR(255),
    foto_perfil BYTEA,
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE TABLE usuario_roles (
    usuario_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (usuario_id, role_id),
    CONSTRAINT fk_usuario_roles_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_usuario_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE planos (
    id UUID PRIMARY KEY,
    nome_projeto VARCHAR(150) NOT NULL,
    versao VARCHAR(30) NOT NULL,
    objetivo VARCHAR(2000) NOT NULL,
    visao_geral VARCHAR(5000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    criado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE participacoes_plano (
    id UUID PRIMARY KEY,
    plano_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    criado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_participacao_plano_usuario UNIQUE (plano_id, usuario_id),
    CONSTRAINT fk_participacao_plano_plano
        FOREIGN KEY (plano_id) REFERENCES planos (id),
    CONSTRAINT fk_participacao_plano_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE participacao_plano_papeis (
    participacao_id UUID NOT NULL,
    papel VARCHAR(40) NOT NULL,
    PRIMARY KEY (participacao_id, papel),
    CONSTRAINT fk_participacao_papeis_participacao
        FOREIGN KEY (participacao_id) REFERENCES participacoes_plano (id)
);

CREATE TABLE documentos (
    id UUID PRIMARY KEY,
    plano_id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    versao VARCHAR(30) NOT NULL,
    conteudo BYTEA NOT NULL,
    tipo_arquivo VARCHAR(255) NOT NULL,
    tamanho BIGINT NOT NULL,
    classificacao VARCHAR(20) NOT NULL,
    CONSTRAINT fk_documento_plano
        FOREIGN KEY (plano_id) REFERENCES planos (id)
);

CREATE TABLE artefatos (
    id UUID PRIMARY KEY,
    documento_id UUID NOT NULL,
    auditor_participacao_id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    versao VARCHAR(30) NOT NULL,
    data_planejada DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    criado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_artefato_documento
        FOREIGN KEY (documento_id) REFERENCES documentos (id),
    CONSTRAINT fk_artefato_auditor
        FOREIGN KEY (auditor_participacao_id) REFERENCES participacoes_plano (id)
);

CREATE TABLE checklists (
    id UUID PRIMARY KEY,
    artefato_id UUID NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(2000) NOT NULL,
    versao VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    versao_registro BIGINT NOT NULL,
    CONSTRAINT uk_checklist_artefato_versao UNIQUE (artefato_id, versao),
    CONSTRAINT fk_checklist_artefato
        FOREIGN KEY (artefato_id) REFERENCES artefatos (id)
);

CREATE TABLE itens_checklist (
    id UUID PRIMARY KEY,
    checklist_id UUID NOT NULL,
    ordem INTEGER NOT NULL,
    pergunta VARCHAR(1000) NOT NULL,
    orientacao VARCHAR(2000),
    origem VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_item_checklist_ordem UNIQUE (checklist_id, ordem),
    CONSTRAINT fk_item_checklist_checklist
        FOREIGN KEY (checklist_id) REFERENCES checklists (id)
);

CREATE TABLE auditorias (
    id UUID PRIMARY KEY,
    artefato_id UUID NOT NULL,
    checklist_id UUID NOT NULL,
    auditor_participacao_id UUID NOT NULL,
    data_inicio TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    data_fim TIMESTAMP(6) WITHOUT TIME ZONE,
    conformes INTEGER NOT NULL,
    nao_conformes INTEGER NOT NULL,
    nao_aplicaveis INTEGER NOT NULL,
    aderencia_percentual NUMERIC(5, 2),
    status VARCHAR(30) NOT NULL,
    versao_registro BIGINT NOT NULL,
    CONSTRAINT fk_auditoria_artefato
        FOREIGN KEY (artefato_id) REFERENCES artefatos (id),
    CONSTRAINT fk_auditoria_checklist
        FOREIGN KEY (checklist_id) REFERENCES checklists (id),
    CONSTRAINT fk_auditoria_auditor
        FOREIGN KEY (auditor_participacao_id) REFERENCES participacoes_plano (id)
);

CREATE TABLE respostas_auditoria (
    id UUID PRIMARY KEY,
    auditoria_id UUID NOT NULL,
    item_checklist_id UUID NOT NULL,
    resultado VARCHAR(30) NOT NULL,
    observacao VARCHAR(3000),
    respondido_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    versao_registro BIGINT NOT NULL,
    CONSTRAINT uk_resposta_auditoria_item UNIQUE (auditoria_id, item_checklist_id),
    CONSTRAINT fk_resposta_auditoria_auditoria
        FOREIGN KEY (auditoria_id) REFERENCES auditorias (id),
    CONSTRAINT fk_resposta_auditoria_item
        FOREIGN KEY (item_checklist_id) REFERENCES itens_checklist (id)
);

CREATE TABLE nao_conformidades (
    id UUID PRIMARY KEY,
    resposta_id UUID NOT NULL,
    responsavel_participacao_id UUID NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    classificacao VARCHAR(20) NOT NULL,
    descricao VARCHAR(3000) NOT NULL,
    acao_corretiva VARCHAR(3000) NOT NULL,
    identificado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    prazo_resolucao_horas INTEGER NOT NULL,
    enviada_em TIMESTAMP(6) WITH TIME ZONE,
    prazo_em TIMESTAMP(6) WITH TIME ZONE,
    concluida_em TIMESTAMP(6) WITH TIME ZONE,
    atualizado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL,
    versao_registro BIGINT NOT NULL,
    CONSTRAINT uk_nc_resposta UNIQUE (resposta_id),
    CONSTRAINT fk_nc_resposta
        FOREIGN KEY (resposta_id) REFERENCES respostas_auditoria (id),
    CONSTRAINT fk_nc_responsavel
        FOREIGN KEY (responsavel_participacao_id) REFERENCES participacoes_plano (id)
);

CREATE INDEX idx_nc_status_prazo
    ON nao_conformidades (status, prazo_em);

CREATE TABLE comunicacoes_nc (
    id UUID PRIMARY KEY,
    nao_conformidade_id UUID NOT NULL,
    chave_idempotencia VARCHAR(100) NOT NULL,
    destinatario VARCHAR(254) NOT NULL,
    assunto VARCHAR(200) NOT NULL,
    corpo VARCHAR(10000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    tentativas INTEGER NOT NULL,
    criada_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    ultima_tentativa_em TIMESTAMP(6) WITH TIME ZONE,
    enviada_em TIMESTAMP(6) WITH TIME ZONE,
    detalhe_erro VARCHAR(500),
    versao_registro BIGINT NOT NULL,
    CONSTRAINT uk_comunicacao_nc_idempotencia
        UNIQUE (nao_conformidade_id, chave_idempotencia),
    CONSTRAINT fk_comunicacao_nc_nao_conformidade
        FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id)
);

CREATE TABLE resolucoes_nc (
    id UUID PRIMARY KEY,
    nao_conformidade_id UUID NOT NULL,
    responsavel_participacao_id UUID NOT NULL,
    descricao VARCHAR(5000) NOT NULL,
    evidencia VARCHAR(2000),
    status VARCHAR(25) NOT NULL,
    informada_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    auditor_participacao_id UUID,
    observacao_auditor VARCHAR(3000),
    validada_em TIMESTAMP(6) WITH TIME ZONE,
    versao_registro BIGINT NOT NULL,
    CONSTRAINT fk_resolucao_nc_nao_conformidade
        FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id),
    CONSTRAINT fk_resolucao_nc_responsavel
        FOREIGN KEY (responsavel_participacao_id) REFERENCES participacoes_plano (id),
    CONSTRAINT fk_resolucao_nc_auditor
        FOREIGN KEY (auditor_participacao_id) REFERENCES participacoes_plano (id)
);

CREATE TABLE escalonamentos_nc (
    id UUID PRIMARY KEY,
    nao_conformidade_id UUID NOT NULL,
    chave_idempotencia VARCHAR(100) NOT NULL,
    nivel VARCHAR(10) NOT NULL,
    responsavel_participacao_id UUID NOT NULL,
    auditor_participacao_id UUID NOT NULL,
    observacao VARCHAR(3000),
    prazo_horas INTEGER NOT NULL,
    escalonado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    prazo_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    versao_registro BIGINT NOT NULL,
    CONSTRAINT uk_escalonamento_nc_idempotencia
        UNIQUE (nao_conformidade_id, chave_idempotencia),
    CONSTRAINT uk_escalonamento_nc_nivel
        UNIQUE (nao_conformidade_id, nivel),
    CONSTRAINT fk_escalonamento_nc_nao_conformidade
        FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id),
    CONSTRAINT fk_escalonamento_nc_responsavel
        FOREIGN KEY (responsavel_participacao_id) REFERENCES participacoes_plano (id),
    CONSTRAINT fk_escalonamento_nc_auditor
        FOREIGN KEY (auditor_participacao_id) REFERENCES participacoes_plano (id)
);

CREATE INDEX idx_escalonamento_nc_data
    ON escalonamentos_nc (nao_conformidade_id, escalonado_em);

CREATE TABLE notificacoes (
    id UUID PRIMARY KEY,
    destinatario_participacao_id UUID NOT NULL,
    nao_conformidade_id UUID NOT NULL,
    chave_evento VARCHAR(150) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensagem VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    criada_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    lida_em TIMESTAMP(6) WITH TIME ZONE,
    versao_registro BIGINT NOT NULL,
    CONSTRAINT uk_notificacao_chave_evento UNIQUE (chave_evento),
    CONSTRAINT fk_notificacao_destinatario
        FOREIGN KEY (destinatario_participacao_id) REFERENCES participacoes_plano (id),
    CONSTRAINT fk_notificacao_nao_conformidade
        FOREIGN KEY (nao_conformidade_id) REFERENCES nao_conformidades (id)
);

CREATE INDEX idx_notificacao_destinatario_criada
    ON notificacoes (destinatario_participacao_id, criada_em);
