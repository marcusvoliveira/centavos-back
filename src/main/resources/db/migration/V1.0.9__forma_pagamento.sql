-- Forma de pagamento da assinatura da plataforma (PIX ou cartão)
-- Dados do cartão nunca são gravados no BD
ALTER TABLE projects ADD COLUMN forma_pagamento VARCHAR(20);
ALTER TABLE projects ADD COLUMN forma_pagamento_pix_key VARCHAR(100);
