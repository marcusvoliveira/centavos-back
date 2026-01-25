-- Create projects table
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create user_projects table (many-to-many relationship)
CREATE TABLE user_projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_projects_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_projects_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_project UNIQUE (user_id, project_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_user_projects_user_id ON user_projects(user_id);
CREATE INDEX idx_user_projects_project_id ON user_projects(project_id);
CREATE INDEX idx_user_projects_role ON user_projects(role);
CREATE INDEX idx_projects_active ON projects(active);

-- Insert a default project for existing system
INSERT INTO projects (name, description, active, created_at, updated_at)
VALUES ('Default Project', 'Projeto padrão do sistema', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Associate all existing users to the default project with their current roles
INSERT INTO user_projects (user_id, project_id, role, created_at)
SELECT u.id, p.id, u.role, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN projects p
WHERE p.name = 'Default Project';
