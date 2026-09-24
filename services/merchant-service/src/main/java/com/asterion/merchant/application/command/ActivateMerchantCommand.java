package com.asterion.merchant.application.command;

import java.util.UUID;

public record ActivateMerchantCommand(
        UUID merchantId
) {
}