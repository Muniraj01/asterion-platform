package com.asterion.testing;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class RedisContainerSupport {

    private static final DockerImageName REDIS_IMAGE =
            DockerImageName.parse("redis:7.4-alpine");

    protected static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(REDIS_IMAGE)
                    .withExposedPorts(6379);

    static {
        REDIS_CONTAINER.start();

        System.setProperty(
                "spring.data.redis.host",
                REDIS_CONTAINER.getHost()
        );

        System.setProperty(
                "spring.data.redis.port",
                REDIS_CONTAINER.getMappedPort(6379).toString()
        );
    }
}