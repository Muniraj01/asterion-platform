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

    private RiskDecision riskDecision;
    private String riskReasonCode;
    private Instant riskAssessedAt;

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
            Instant processingLeaseUntil,
            RiskDecision riskDecision,
            String riskReasonCode,
            Instant riskAssessedAt) {

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
        this.riskDecision = riskDecision;
        this.riskReasonCode = riskReasonCode;
        this.riskAssessedAt = riskAssessedAt;

        if (totalAmount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Payment amount must not be negative");
        }

        if (attemptCount < 0) {
            throw new IllegalArgumentException(
                    "Payment attempt count must not be negative");
        }

        if (riskDecision == null
                && (riskReasonCode != null || riskAssessedAt != null)) {
            throw new IllegalArgumentException(
                    "Risk metadata requires a risk decision");
        }

        if (riskDecision != null
                && (riskReasonCode == null
                || riskReasonCode.isBlank()
                || riskAssessedAt == null)) {
            throw new IllegalArgumentException(
                    "A risk decision requires a reason and assessment timestamp");
        }

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
                null,
                null,
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
                null,
                null,
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
                attemptCount,
                nextAttemptAt,
                processingLeaseUntil,
                null,
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
            Instant processingLeaseUntil,
            RiskDecision riskDecision,
            String riskReasonCode,
            Instant riskAssessedAt) {

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
                processingLeaseUntil,
                riskDecision,
                riskReasonCode,
                riskAssessedAt
        );
    }

    public void startProcessing(Instant now) {
        transitionTo(PaymentStatus.PROCESSING, now);
    }

    public void recordRiskAssessment(
            RiskDecision decision,
            String reasonCode,
            Instant assessedAt) {

        if (status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Risk assessment can only be recorded while processing");
        }

        if (riskDecision != null) {
            throw new IllegalStateException(
                    "Risk assessment has already been recorded");
        }

        Objects.requireNonNull(decision, "Risk decision must not be null");

        if (reasonCode == null || reasonCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Risk reason code must not be blank");
        }

        this.riskDecision = decision;
        this.riskReasonCode = reasonCode.trim();
        this.riskAssessedAt = Objects.requireNonNull(assessedAt);
        this.updatedAt = assessedAt;
    }

    public void rejectForRisk(Instant now) {
        if (riskDecision != RiskDecision.REJECT) {
            throw new IllegalStateException(
                    "Payment can only be risk-rejected after a REJECT decision");
        }

        transitionTo(PaymentStatus.RISK_REJECTED, now);
    }

    public void holdForRiskReview(Instant now) {
        if (riskDecision != RiskDecision.REVIEW) {
            throw new IllegalStateException(
                    "Payment can only be held after a REVIEW decision");
        }

        transitionTo(PaymentStatus.RISK_REVIEW, now);
    }

    public void succeed(Instant now) {
        ensureApprovedOrLegacyUnassessed();
        transitionTo(PaymentStatus.SUCCEEDED, now);
    }

    public void fail(Instant now) {
        ensureApprovedOrLegacyUnassessed();
        transitionTo(PaymentStatus.FAILED, now);
    }

    public void scheduleRetry(Instant retryAt, Instant now) {
        ensureApprovedOrLegacyUnassessed();

        if (status != PaymentStatus.PROCESSING) {
            throw new InvalidPaymentStateTransitionException(
                    status, PaymentStatus.RETRYABLE);
        }

        status = PaymentStatus.RETRYABLE;
        updatedAt = Objects.requireNonNull(now);
        nextAttemptAt = Objects.requireNonNull(retryAt);
        processingLeaseUntil = null;
    }

    private void ensureApprovedOrLegacyUnassessed() {
        if (riskDecision != null && riskDecision != RiskDecision.APPROVE) {
            throw new IllegalStateException(
                    "Payment cannot be processed after a non-approval risk decision");
        }
    }

    private void transitionTo(PaymentStatus targetStatus, Instant now) {
        boolean valid = switch (status) {
            case INITIATED, RETRYABLE ->
                    targetStatus == PaymentStatus.PROCESSING
                            || targetStatus == PaymentStatus.FAILED;

            case PROCESSING ->
                    targetStatus == PaymentStatus.SUCCEEDED
                            || targetStatus == PaymentStatus.FAILED
                            || targetStatus == PaymentStatus.RISK_REJECTED
                            || targetStatus == PaymentStatus.RISK_REVIEW;

            case SUCCEEDED, FAILED, RISK_REJECTED, RISK_REVIEW -> false;
        };

        if (!valid) {
            throw new InvalidPaymentStateTransitionException(
                    status, targetStatus);
        }

        status = targetStatus;
        updatedAt = Objects.requireNonNull(now);
        nextAttemptAt = null;
        processingLeaseUntil = null;
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

    public RiskDecision riskDecision() {
        return riskDecision;
    }

    public String riskReasonCode() {
        return riskReasonCode;
    }

    public Instant riskAssessedAt() {
        return riskAssessedAt;
    }
}