package com.asterion.merchant.application.command;

import java.util.UUID;

public record ReactivateMerchantCommand(
        UUID merchantId,
        UUID authenticatedUserId
) {
}