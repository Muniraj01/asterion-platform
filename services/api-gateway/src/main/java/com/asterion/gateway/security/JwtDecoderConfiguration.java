package com.asterion.gateway.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
public class JwtDecoderConfiguration {

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${asterion.security.jwt.secret}") String secret) {

        SecretKey key = new SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return NimbusReactiveJwtDecoder
                .withSecretKey(key)
                .build();
    }
}