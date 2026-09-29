ALTER TABLE parcela
    ALTER COLUMN data_pagamento TYPE DATE USING data_pagamento::DATE;

ALTER TABLE parcela
    ADD CONSTRAINT uk_parcela_pagamento_numero UNIQUE (pagamento_id, numero);
