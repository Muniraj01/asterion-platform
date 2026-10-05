package com.asterion.order.application.model;

import java.util.UUID;

public record MerchantValidationResult(UUID merchantId, String status) {

    public boolean active() {
        return "ACTIVE".equals(status);
    }
}