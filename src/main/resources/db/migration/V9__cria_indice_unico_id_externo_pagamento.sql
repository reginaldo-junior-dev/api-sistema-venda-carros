-- O webhook da Stripe busca o pagamento pelo id do PaymentIntent; cada PaymentIntent pertence a um só pagamento.
-- Pagamentos sem integração (id_externo NULL) não conflitam: no PostgreSQL NULLs não se repetem no índice único
CREATE UNIQUE INDEX idx_pagamento_id_externo ON pagamento(id_externo);
