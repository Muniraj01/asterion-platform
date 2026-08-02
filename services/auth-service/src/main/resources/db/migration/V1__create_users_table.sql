CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(320) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       active BOOLEAN NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_users_email
    ON users(email);