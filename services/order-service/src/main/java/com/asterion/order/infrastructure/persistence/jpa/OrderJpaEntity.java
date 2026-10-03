package com.asterion.order.infrastructure.persistence.jpa;

import com.asterion.order.domain.model.OrderStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrderJpaEntity() {
    }

    public OrderJpaEntity(
            UUID orderId,
            UUID merchantId,
            UUID customerId,
            BigDecimal totalAmount,
            OrderStatus status,
            Instant createdAt) {

        this.orderId = orderId;
        this.merchantId = merchantId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}