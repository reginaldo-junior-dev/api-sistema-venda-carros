CREATE TABLE compra (
    id UUID PRIMARY KEY,
    valor_total DECIMAL(10, 2) NOT NULL,
    status VARCHAR(15) NOT NULL,
    data_compra TIMESTAMP NOT NULL,
    cliente_id UUID NOT NULL,
    carro_id UUID NOT NULL,

    CONSTRAINT fk_compra_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(id),

    CONSTRAINT fk_compra_carro FOREIGN KEY (carro_id) REFERENCES carro(id)
);

CREATE INDEX idx_compra_cliente_id ON compra(cliente_id);
CREATE INDEX idx_compra_carro_id ON compra(carro_id);

CREATE TABLE pagamento (
    id UUID PRIMARY KEY,
    valor DECIMAL(10, 2) NOT NULL,
    metodo VARCHAR(20) NOT NULL,
    status VARCHAR(15) NOT NULL,
    data_pagamento TIMESTAMP,
    id_externo VARCHAR(100),
    compra_id UUID NOT NULL,

    CONSTRAINT fk_pagamento_compra FOREIGN KEY (compra_id) REFERENCES compra(id)
);

CREATE INDEX idx_pagamento_compra_id ON pagamento(compra_id);

CREATE TABLE parcela (
    id UUID PRIMARY KEY,
    numero INTEGER NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    status VARCHAR(15) NOT NULL,
    data_pagamento TIMESTAMP,
    data_vencimento DATE NOT NULL,
    pagamento_id UUID NOT NULL,

    CONSTRAINT fk_parcela_pagamento FOREIGN KEY (pagamento_id) REFERENCES pagamento(id)
);

CREATE INDEX idx_parcela_pagamento_id ON parcela(pagamento_id);
