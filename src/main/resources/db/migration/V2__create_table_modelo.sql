CREATE TABLE modelo (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    marca_id UUID NOT NULL,
    CONSTRAINT fk_modelo_marca FOREIGN KEY (marca_id) REFERENCES marca(id),
    CONSTRAINT uk_modelo_nome_marca UNIQUE (nome, marca_id)
);

CREATE INDEX idx_modelo_marca_id ON modelo(marca_id);
