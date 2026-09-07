package com.asterion.merchant.application.command;

import java.util.UUID;

public record CreateMerchantCommand(
        UUID ownerUserId,
        String businessName,
        String legalName,
        String contactEmail
) {
}