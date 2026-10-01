package com.asterion.merchant.application.command;

import java.util.UUID;

public record SuspendMerchantCommand(
        UUID merchantId,
        UUID authenticatedUserId
) {
}