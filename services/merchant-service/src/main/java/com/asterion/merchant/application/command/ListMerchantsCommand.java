package com.asterion.merchant.application.command;

import java.util.UUID;

public record ListMerchantsCommand(
        UUID authenticatedUserId,
        int page,
        int size
) {
}