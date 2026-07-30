package com.asterion.testing;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class RedisContainerSupport {

    @Container
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    protected String redisHost() {
        return REDIS.getHost();
    }

    protected Integer redisPort() {
        return REDIS.getMappedPort(6379);
    }
}