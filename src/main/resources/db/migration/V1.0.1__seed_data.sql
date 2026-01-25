-- Insert admin user (password: admin123)
-- BCrypt hash for 'admin123'
INSERT INTO users (name, email, password, role, email_verified, created_at, updated_at)
VALUES (
    'Administrator',
    'admin@incentive.com',
    '$2a$10$LzPkfGh/rqGz5tTP7r.dUeEUerELZh.tlLUIQldSru7a2muIRKqBu',
    'ADMIN',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
