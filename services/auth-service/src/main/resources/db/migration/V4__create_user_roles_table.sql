CREATE TABLE user_roles (
                            user_id UUID NOT NULL,
                            role VARCHAR(50) NOT NULL,

                            CONSTRAINT pk_user_roles
                                PRIMARY KEY (user_id, role),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role
    ON user_roles(role);

INSERT INTO user_roles (user_id, role)
SELECT id, 'USER'
FROM users;