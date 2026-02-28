-- Agents (role = AGENT) are created by admins and don't need a password or CPF.
-- Password and CPF validation is enforced at the DTO level for regular user registration.
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE users ALTER COLUMN cpf DROP NOT NULL;
