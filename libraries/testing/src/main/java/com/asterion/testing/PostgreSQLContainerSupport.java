package com.asterion.testing;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class PostgreSQLContainerSupport {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("asterion")
                    .withUsername("asterion")
                    .withPassword("asterion");

    protected String jdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }

    protected String username() {
        return POSTGRES.getUsername();
    }

    protected String password() {
        return POSTGRES.getPassword();
    }
}