package com.asterion.payment.infrastructure.persistence;

import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.PaymentStatus;
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

        return entity;
    }

    public void updateFromDomain(Payment payment) {
        this.status = payment.status();
        this.updatedAt = payment.updatedAt();
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
                updatedAt
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