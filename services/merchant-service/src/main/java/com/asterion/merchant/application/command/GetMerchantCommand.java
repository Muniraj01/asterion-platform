package com.asterion.merchant.application.command;

import java.util.UUID;

public record GetMerchantCommand(
        UUID merchantId,
        UUID authenticatedUserId
) {
}