CREATE TABLE feriados_plano (
    id UUID PRIMARY KEY,
    plano_id UUID NOT NULL,
    data DATE NOT NULL,
    nome VARCHAR(120) NOT NULL,
    CONSTRAINT uk_feriado_plano_data UNIQUE (plano_id, data),
    CONSTRAINT fk_feriado_plano
        FOREIGN KEY (plano_id) REFERENCES planos (id) ON DELETE CASCADE
);

CREATE INDEX idx_feriado_plano_data
    ON feriados_plano (plano_id, data);
