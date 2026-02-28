-- Hibernate 6 (Quarkus 3+) expects sequences named {table_name}_seq,
-- but PostgreSQL BIGSERIAL/SERIAL creates them as {table_name}_id_seq.
-- Renaming keeps the sequence linked to the column (DEFAULT still works)
-- while Hibernate can find the sequence by its expected name.

ALTER SEQUENCE IF EXISTS users_id_seq RENAME TO users_seq;
ALTER SEQUENCE IF EXISTS projects_id_seq RENAME TO projects_seq;
ALTER SEQUENCE IF EXISTS user_projects_id_seq RENAME TO user_projects_seq;
ALTER SEQUENCE IF EXISTS banks_id_seq RENAME TO banks_seq;
ALTER SEQUENCE IF EXISTS addresses_id_seq RENAME TO addresses_seq;
ALTER SEQUENCE IF EXISTS bank_statements_id_seq RENAME TO bank_statements_seq;
ALTER SEQUENCE IF EXISTS donations_id_seq RENAME TO donations_seq;
