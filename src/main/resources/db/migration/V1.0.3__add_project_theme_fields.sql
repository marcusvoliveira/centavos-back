-- Add theme customization fields to projects table

ALTER TABLE projects ADD COLUMN slug VARCHAR(50) UNIQUE;
ALTER TABLE projects ADD COLUMN primary_color VARCHAR(7) DEFAULT '#c41230';
ALTER TABLE projects ADD COLUMN secondary_color VARCHAR(7) DEFAULT '#ffffff';
ALTER TABLE projects ADD COLUMN background_color VARCHAR(7) DEFAULT '#f3f4f6';
ALTER TABLE projects ADD COLUMN logo_url VARCHAR(500);
ALTER TABLE projects ADD COLUMN hero_image_url VARCHAR(500);

-- Create index for slug lookups
CREATE INDEX idx_projects_slug ON projects(slug);

-- Update default project with a slug
UPDATE projects SET slug = 'default' WHERE name = 'Default Project';
