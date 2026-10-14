package com.asterion.payment.domain.model;

public enum PaymentStatus {
    INITIATED,
    PROCESSING,
    RETRYABLE,
    SUCCEEDED,
    FAILED
}