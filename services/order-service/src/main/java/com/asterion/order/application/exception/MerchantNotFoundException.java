package com.asterion.order.application.exception;

import java.util.UUID;

public class MerchantNotFoundException extends RuntimeException {

    public MerchantNotFoundException(UUID merchantId) {
        super("Merchant not found: " + merchantId);
    }
}