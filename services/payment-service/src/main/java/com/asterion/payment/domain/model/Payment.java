package com.asterion.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Payment {

    private final UUID paymentId;
    private final UUID orderId;
    private final UUID merchantId;
    private final UUID customerId;
    private final UUID sourceEventId;
    private final BigDecimal totalAmount;
    private final Instant createdAt;

    private PaymentStatus status;
    private Instant updatedAt;

    private Payment(
            UUID paymentId,
            UUID orderId,
            UUID merchantId,
            UUID customerId,
            UUID sourceEventId,
            BigDecimal totalAmount,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt) {

        this.paymentId = Objects.requireNonNull(paymentId);
        this.orderId = Objects.requireNonNull(orderId);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.customerId = Objects.requireNonNull(customerId);
        this.sourceEventId = Objects.requireNonNull(sourceEventId);
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);

        if (totalAmount.signum() < 0)
            throw new IllegalArgumentException("Payment amount must not be negative");
    }

    public static Payment initiate(
            UUID paymentId,
            UUID orderId,
            UUID merchantId,
            UUID customerId,
            UUID sourceEventId,
            BigDecimal totalAmount,
            Instant occurredAt) {

        return new Payment(
                paymentId,
                orderId,
                merchantId,
                customerId,
                sourceEventId,
                totalAmount,
                PaymentStatus.INITIATED,
                occurredAt,
                occurredAt
        );
    }

    public static Payment rehydrate(
            UUID paymentId,
            UUID orderId,
            UUID merchantId,
            UUID customerId,
            UUID sourceEventId,
            BigDecimal totalAmount,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt) {

        return new Payment(
                paymentId,
                orderId,
                merchantId,
                customerId,
                sourceEventId,
                totalAmount,
                status,
                createdAt,
                updatedAt
        );
    }

    public void startProcessing(Instant now) {
        transitionTo(PaymentStatus.PROCESSING, now);
    }

    public void succeed(Instant now) {
        transitionTo(PaymentStatus.SUCCEEDED, now);
    }

    public void fail(Instant now) {
        transitionTo(PaymentStatus.FAILED, now);
    }

    private void transitionTo(PaymentStatus targetStatus, Instant now) {

        boolean valid = switch (status) {
            case INITIATED ->
                    targetStatus == PaymentStatus.PROCESSING
                            || targetStatus == PaymentStatus.FAILED;

            case PROCESSING ->
                    targetStatus == PaymentStatus.SUCCEEDED
                            || targetStatus == PaymentStatus.FAILED;

            case SUCCEEDED, FAILED -> false;
        };

        if (!valid)
            throw new InvalidPaymentStateTransitionException(status, targetStatus);

        status = targetStatus;
        updatedAt = Objects.requireNonNull(now);
    }

    public UUID paymentId() {
        return paymentId;
    }

    public UUID orderId() {
        return orderId;
    }

    public UUID merchantId() {
        return merchantId;
    }

    public UUID customerId() {
        return customerId;
    }

    public UUID sourceEventId() {
        return sourceEventId;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public PaymentStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}