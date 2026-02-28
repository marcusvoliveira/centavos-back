-- Banks table
CREATE TABLE banks (
    id SERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO banks (code, name) VALUES
    ('001', 'Banco do Brasil'),
    ('033', 'Santander'),
    ('077', 'Banco Inter'),
    ('104', 'Caixa Econômica Federal'),
    ('237', 'Bradesco'),
    ('260', 'Nu Pagamentos (Nubank)'),
    ('290', 'PagBank'),
    ('323', 'Mercado Pago'),
    ('336', 'C6 Bank'),
    ('341', 'Itaú'),
    ('380', 'PicPay'),
    ('422', 'Safra'),
    ('748', 'Sicredi'),
    ('756', 'Sicoob');

-- Agent participation percentage in each project
ALTER TABLE user_projects ADD COLUMN participation NUMERIC(5,2);

-- PIX key for agents/users
ALTER TABLE users ADD COLUMN pix_key VARCHAR(100);
