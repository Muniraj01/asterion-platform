package com.asterion.payment.infrastructure.persistence;

import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.PaymentStatus;
import com.asterion.payment.domain.model.RiskDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "processing_lease_until")
    private Instant processingLeaseUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_decision", length = 16)
    private RiskDecision riskDecision;

    @Column(name = "risk_reason_code", length = 100)
    private String riskReasonCode;

    @Column(name = "risk_assessed_at")
    private Instant riskAssessedAt;

    protected PaymentJpaEntity() {
    }

    public static PaymentJpaEntity fromDomain(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();

        entity.paymentId = payment.paymentId();
        entity.orderId = payment.orderId();
        entity.merchantId = payment.merchantId();
        entity.customerId = payment.customerId();
        entity.sourceEventId = payment.sourceEventId();
        entity.totalAmount = payment.totalAmount();
        entity.status = payment.status();
        entity.createdAt = payment.createdAt();
        entity.updatedAt = payment.updatedAt();
        entity.attemptCount = payment.attemptCount();
        entity.nextAttemptAt = payment.nextAttemptAt();
        entity.processingLeaseUntil = payment.processingLeaseUntil();
        entity.riskDecision = payment.riskDecision();
        entity.riskReasonCode = payment.riskReasonCode();
        entity.riskAssessedAt = payment.riskAssessedAt();

        return entity;
    }

    public void updateFromDomain(Payment payment) {
        this.status = payment.status();
        this.updatedAt = payment.updatedAt();
        this.attemptCount = payment.attemptCount();
        this.nextAttemptAt = payment.nextAttemptAt();
        this.processingLeaseUntil = payment.processingLeaseUntil();
        this.riskDecision = payment.riskDecision();
        this.riskReasonCode = payment.riskReasonCode();
        this.riskAssessedAt = payment.riskAssessedAt();
    }

    public Payment toDomain() {
        return Payment.rehydrate(
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

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }
}