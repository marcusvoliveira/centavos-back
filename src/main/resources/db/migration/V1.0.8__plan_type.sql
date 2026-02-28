-- Plan type for the project billing (MENSAL, SEMESTRAL, ANUAL)
ALTER TABLE projects ADD COLUMN plan_type VARCHAR(20);
