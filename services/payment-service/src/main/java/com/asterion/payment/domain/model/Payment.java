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
    private int attemptCount;
    private Instant nextAttemptAt;
    private Instant processingLeaseUntil;

    private Payment(
            UUID paymentId,
            UUID orderId,
            UUID merchantId,
            UUID customerId,
            UUID sourceEventId,
            BigDecimal totalAmount,
            PaymentStatus status,
            Instant createdAt,
            Instant updatedAt,
            int attemptCount,
            Instant nextAttemptAt,
            Instant processingLeaseUntil) {

        this.paymentId = Objects.requireNonNull(paymentId);
        this.orderId = Objects.requireNonNull(orderId);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.customerId = Objects.requireNonNull(customerId);
        this.sourceEventId = Objects.requireNonNull(sourceEventId);
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.nextAttemptAt = nextAttemptAt;
        this.processingLeaseUntil = processingLeaseUntil;

        if (totalAmount.signum() < 0)
            throw new IllegalArgumentException("Payment amount must not be negative");

        if (attemptCount < 0)
            throw new IllegalArgumentException("Payment attempt count must not be negative");

        this.attemptCount = attemptCount;
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
                occurredAt,
                0,
                null,
                null
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

        return rehydrate(
                paymentId,
                orderId,
                merchantId,
                customerId,
                sourceEventId,
                totalAmount,
                status,
                createdAt,
                updatedAt,
                0,
                null,
                null
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
            Instant updatedAt,
            int attemptCount,
            Instant nextAttemptAt,
            Instant processingLeaseUntil) {

        return new Payment(
                paymentId,
                orderId,
                merchantId,
                customerId,
                sourceEventId,
                totalAmount,
                status,
                createdAt,
                updatedAt,
                attemptCount,
                nextAttemptAt,
                processingLeaseUntil
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

    public void scheduleRetry(Instant retryAt, Instant now) {
        if (status != PaymentStatus.PROCESSING)
            throw new InvalidPaymentStateTransitionException(status, PaymentStatus.RETRYABLE);

        status = PaymentStatus.RETRYABLE;
        updatedAt = Objects.requireNonNull(now);
        nextAttemptAt = Objects.requireNonNull(retryAt);
        processingLeaseUntil = null;
    }

    private void transitionTo(PaymentStatus targetStatus, Instant now) {
        boolean valid = switch (status) {
            case INITIATED, RETRYABLE ->
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

        if (targetStatus == PaymentStatus.PROCESSING) {
            nextAttemptAt = null;
        } else {
            nextAttemptAt = null;
            processingLeaseUntil = null;
        }
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

    public int attemptCount() {
        return attemptCount;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant processingLeaseUntil() {
        return processingLeaseUntil;
    }
}