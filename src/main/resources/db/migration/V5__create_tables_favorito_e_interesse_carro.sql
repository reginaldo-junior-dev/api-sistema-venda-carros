CREATE TABLE favorito (
    id UUID PRIMARY KEY,
    data_favorito TIMESTAMP NOT NULL,
    carro_id UUID NOT NULL,
    cliente_id UUID NOT NULL,

    CONSTRAINT fk_favorito_carro FOREIGN KEY (carro_id) REFERENCES carro(id),

    CONSTRAINT fk_favorito_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),

    CONSTRAINT uk_favorito_cliente_carro UNIQUE (cliente_id, carro_id)
);

CREATE INDEX idx_favorito_carro_id ON favorito(carro_id);

CREATE TABLE interesse_carro (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telefone VARCHAR(11) NOT NULL,
    mensagem VARCHAR(500) NOT NULL,
    status VARCHAR(15) NOT NULL,
    data_interesse TIMESTAMP NOT NULL,
    cliente_id UUID NOT NULL,
    carro_id UUID NOT NULL,

    CONSTRAINT fk_interesse_carro_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),

    CONSTRAINT fk_interesse_carro_carro FOREIGN KEY (carro_id) REFERENCES carro(id)
);

CREATE INDEX idx_interesse_carro_cliente_id ON interesse_carro(cliente_id);
CREATE INDEX idx_interesse_carro_carro_id ON interesse_carro(carro_id);
