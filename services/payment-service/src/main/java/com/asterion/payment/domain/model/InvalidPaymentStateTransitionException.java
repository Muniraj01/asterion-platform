package com.asterion.payment.domain.model;

public class InvalidPaymentStateTransitionException extends RuntimeException {

    public InvalidPaymentStateTransitionException(PaymentStatus currentStatus,
                                                  PaymentStatus targetStatus) {

        super("Invalid payment state transition: " + currentStatus + " -> " + targetStatus);
    }
}