package com.asterion.order.domain.model;

import com.asterion.order.application.exception.InvalidOrderStateTransitionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public class Order {

    private final UUID orderId;
    private final UUID merchantId;
    private final UUID customerId;
    private final BigDecimal totalAmount;
    private final Instant createdAt;
    private OrderStatus status;

    private Order(
            UUID orderId,
            UUID merchantId,
            UUID customerId,
            BigDecimal totalAmount,
            OrderStatus status,
            Instant createdAt) {

        this.orderId = Objects.requireNonNull(orderId,
                "orderId must not be null");
        this.merchantId = Objects.requireNonNull(merchantId,
                "merchantId must not be null");
        this.customerId = Objects.requireNonNull(customerId,
                "customerId must not be null");

        if (totalAmount == null)
            throw new IllegalArgumentException("totalAmount must not be null");

        if (totalAmount.signum() < 0)
            throw new IllegalArgumentException("totalAmount must not be negative");

        this.totalAmount = totalAmount;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt,
                "createdAt must not be null");
    }

    public static Order create(
            UUID merchantId,
            UUID customerId,
            BigDecimal totalAmount) {

        return new Order(
                UUID.randomUUID(),
                merchantId,
                customerId,
                totalAmount,
                OrderStatus.CREATED,
                Instant.now().truncatedTo(ChronoUnit.MICROS)
        );
    }

    public static Order reconstitute(
            UUID orderId,
            UUID merchantId,
            UUID customerId,
            BigDecimal totalAmount,
            OrderStatus status,
            Instant createdAt) {

        return new Order(
                orderId,
                merchantId,
                customerId,
                totalAmount,
                status,
                createdAt
        );
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    public void complete() {
        transitionTo(OrderStatus.COMPLETED);
    }

    private void transitionTo(OrderStatus targetStatus) {
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");

        if (status != OrderStatus.CREATED)
            throw new InvalidOrderStateTransitionException(status, targetStatus);

        status = targetStatus;
    }

    public UUID id() {
        return orderId;
    }

    public UUID merchantId() {
        return merchantId;
    }

    public UUID customerId() {
        return customerId;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }
}