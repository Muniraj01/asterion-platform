package com.asterion.order.infrastructure.messaging;

public final class PermanentPaymentOutcomeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PermanentPaymentOutcomeException(String message) {
        super(message);
    }

    public PermanentPaymentOutcomeException(String message, Throwable cause) {
        super(message, cause);
    }
}
