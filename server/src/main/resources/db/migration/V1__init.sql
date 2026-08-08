CREATE TABLE users (
    id            UUID PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    BIGINT       NOT NULL,
    updated_at    BIGINT       NOT NULL
);

CREATE TABLE todo_lists (
    id         UUID PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    owner_id   UUID NOT NULL REFERENCES users (id),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE INDEX idx_todo_lists_owner ON todo_lists (owner_id);

CREATE TABLE list_members (
    list_id   UUID NOT NULL REFERENCES todo_lists (id) ON DELETE CASCADE,
    user_id   UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role      VARCHAR(20) NOT NULL,
    joined_at BIGINT NOT NULL,
    PRIMARY KEY (list_id, user_id)
);

CREATE INDEX idx_list_members_user ON list_members (user_id);

CREATE TABLE todos (
    id          UUID PRIMARY KEY,
    list_id     UUID NOT NULL REFERENCES todo_lists (id) ON DELETE CASCADE,
    title       VARCHAR(300) NOT NULL,
    description TEXT NOT NULL,
    completed   BOOLEAN NOT NULL,
    created_by  UUID REFERENCES users (id),
    assigned_to UUID REFERENCES users (id),
    position    BIGINT NOT NULL,
    due_date    BIGINT,
    created_at  BIGINT NOT NULL,
    updated_at  BIGINT NOT NULL
);

CREATE INDEX idx_todos_list ON todos (list_id);
