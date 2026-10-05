package com.asterion.order.application.exception;

import java.util.UUID;

public class MerchantNotActiveException extends RuntimeException {

    public MerchantNotActiveException(UUID merchantId, String status) {
        super("Merchant is not active: " + merchantId + " [" + status + "]");
    }
}