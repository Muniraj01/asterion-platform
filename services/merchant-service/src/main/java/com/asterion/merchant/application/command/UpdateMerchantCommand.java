package com.asterion.merchant.application.command;

import java.util.UUID;

public record UpdateMerchantCommand(
        UUID merchantId,
        UUID authenticatedUserId,
        String businessName,
        String legalName,
        String contactEmail
) {
}