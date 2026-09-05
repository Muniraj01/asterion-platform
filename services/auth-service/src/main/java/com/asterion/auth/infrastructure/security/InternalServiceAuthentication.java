package com.asterion.auth.infrastructure.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InternalServiceAuthentication extends AbstractAuthenticationToken {

    private final String serviceName;

    public InternalServiceAuthentication(String serviceName,
                                         Collection<InternalServicePermission> permissions) {
        super(buildAuthorities(permissions));
        this.serviceName = serviceName;
        setAuthenticated(true);
    }

    private static Collection<GrantedAuthority> buildAuthorities(
            Collection<InternalServicePermission> permissions) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_SERVICE"));

        permissions.stream()
                .map(permission ->
                        new SimpleGrantedAuthority("INTERNAL_" + permission.name()))
                .forEach(authorities::add);

        return authorities;
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