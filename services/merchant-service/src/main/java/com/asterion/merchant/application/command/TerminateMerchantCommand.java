package com.asterion.merchant.application.command;

import java.util.UUID;

public record TerminateMerchantCommand(
        UUID merchantId,
        UUID authenticatedUserId
) {
}