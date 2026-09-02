package com.asterion.auth.infrastructure.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

public class InternalServiceAuthentication extends AbstractAuthenticationToken {

    private final String serviceName;

    public InternalServiceAuthentication(String serviceName) {
        super(AuthorityUtils.createAuthorityList("ROLE_SERVICE"));
        this.serviceName = serviceName;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return serviceName;
    }
}