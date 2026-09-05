package com.asterion.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "asterion.security.internal")
public record InternalServiceProperties(Map<String, Service> services) {

    public record Service(String token,
                          Set<InternalServicePermission> permissions) {
    }
}