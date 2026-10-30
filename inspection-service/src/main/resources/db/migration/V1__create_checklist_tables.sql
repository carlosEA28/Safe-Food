CREATE TABLE checklist_template (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL CHECK (length(trim(name)) > 0),
    description VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE checklist_question (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES checklist_template(id),
    text VARCHAR(1000) NOT NULL CHECK (length(trim(text)) > 0),
    category VARCHAR(150) NOT NULL CHECK (length(trim(category)) > 0),
    position INTEGER NOT NULL CHECK (position > 0),
    severity_if_failed VARCHAR(10) NOT NULL CHECK (severity_if_failed IN ('LOW', 'MEDIUM', 'HIGH')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_checklist_question_template_position UNIQUE (template_id, position)
);

CREATE INDEX idx_checklist_template_active ON checklist_template(active);
CREATE INDEX idx_checklist_question_template ON checklist_question(template_id);
