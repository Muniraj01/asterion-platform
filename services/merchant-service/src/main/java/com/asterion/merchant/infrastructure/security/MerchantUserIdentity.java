package com.asterion.merchant.infrastructure.security;

import java.util.UUID;

public record MerchantUserIdentity(
        UUID userId,
        String email,
        String roles
) {
}