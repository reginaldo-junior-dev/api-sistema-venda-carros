CREATE TABLE carro (
    id UUID PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    modelo_id UUID NOT NULL,
    cor_id UUID NOT NULL,
    categoria_id UUID NOT NULL,
    preco DECIMAL(10, 2) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    condicao VARCHAR(30) NOT NULL,
    ano_fabricacao INTEGER NOT NULL,
    ano_modelo INTEGER NOT NULL,
    quilometragem INTEGER NOT NULL,
    combustivel VARCHAR(15) NOT NULL,
    cambio VARCHAR(15) NOT NULL,
    status VARCHAR(15) NOT NULL,
    CONSTRAINT fk_carro_modelo FOREIGN KEY (modelo_id) REFERENCES modelo(id),
    CONSTRAINT fk_carro_cor FOREIGN KEY (cor_id) REFERENCES cor(id),
    CONSTRAINT fk_carro_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);

CREATE INDEX idx_carro_modelo_id ON carro(modelo_id);
CREATE INDEX idx_carro_cor_id ON carro(cor_id);
CREATE INDEX idx_carro_categoria_id ON carro(categoria_id);

CREATE TABLE imagem_carro (
    id UUID PRIMARY KEY,
    url VARCHAR(250) NOT NULL,
    ordem INTEGER NOT NULL,
    principal BOOLEAN NOT NULL,
    carro_id UUID NOT NULL,
    CONSTRAINT fk_imagem_carro_carro FOREIGN KEY (carro_id) REFERENCES carro(id) ON DELETE CASCADE
);

CREATE INDEX idx_imagem_carro_carro_id ON imagem_carro(carro_id);

CREATE UNIQUE INDEX uk_imagem_carro_principal ON imagem_carro(carro_id) WHERE principal;
