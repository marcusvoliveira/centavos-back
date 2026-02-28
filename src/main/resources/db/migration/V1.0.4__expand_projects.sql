-- Expand logo_url and hero_image_url to TEXT to support base64 images
ALTER TABLE projects ALTER COLUMN logo_url TYPE TEXT;
ALTER TABLE projects ALTER COLUMN hero_image_url TYPE TEXT;

-- Hero section texts
ALTER TABLE projects ADD COLUMN hero_title VARCHAR(200);
ALTER TABLE projects ADD COLUMN hero_subtitle VARCHAR(500);

-- Financial
ALTER TABLE projects ADD COLUMN min_value NUMERIC(10,2);

-- Email templates
ALTER TABLE projects ADD COLUMN email_boas_vindas TEXT;
ALTER TABLE projects ADD COLUMN email_aviso_cobranca TEXT;
ALTER TABLE projects ADD COLUMN email_cobranca TEXT;
ALTER TABLE projects ADD COLUMN email_extrato TEXT;
ALTER TABLE projects ADD COLUMN email_cancelamento TEXT;

-- Payment fields
ALTER TABLE projects ADD COLUMN payment_type VARCHAR(20);
ALTER TABLE projects ADD COLUMN bank_code VARCHAR(10);
ALTER TABLE projects ADD COLUMN bank_agency VARCHAR(20);
ALTER TABLE projects ADD COLUMN bank_account VARCHAR(30);
ALTER TABLE projects ADD COLUMN bank_holder_name VARCHAR(100);
ALTER TABLE projects ADD COLUMN bank_holder_document VARCHAR(20);
ALTER TABLE projects ADD COLUMN pix_key VARCHAR(100);

-- Start date
ALTER TABLE projects ADD COLUMN start_date DATE;
