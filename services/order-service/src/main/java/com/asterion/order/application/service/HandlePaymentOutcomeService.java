package com.asterion.order.application.service;

import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.order.application.port.in.HandlePaymentOutcomeUseCase;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.domain.model.Order;
import com.asterion.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class HandlePaymentOutcomeService implements HandlePaymentOutcomeUseCase {

    private final OrderRepository orderRepository;

    public HandlePaymentOutcomeService(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(
                orderRepository, "orderRepository must not be null");
    }

    @Override
    public void handle(PaymentSucceededEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        validateEvent(
                event.eventId(),
                event.occurredAt(),
                event.paymentId(),
                event.orderId(),
                event.merchantId(),
                event.customerId(),
                event.totalAmount());

        applyOutcome(
                event.orderId(),
                event.merchantId(),
                event.customerId(),
                event.totalAmount(),
                OrderStatus.COMPLETED);
    }

    @Override
    public void handle(PaymentFailedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        validateEvent(
                event.eventId(),
                event.occurredAt(),
                event.paymentId(),
                event.orderId(),
                event.merchantId(),
                event.customerId(),
                event.totalAmount());

        applyOutcome(
                event.orderId(),
                event.merchantId(),
                event.customerId(),
                event.totalAmount(),
                OrderStatus.CANCELLED);
    }

    private void validateEvent(
            UUID eventId,
            Instant occurredAt,
            UUID paymentId,
            UUID orderId,
            UUID merchantId,
            UUID customerId,
            BigDecimal totalAmount) {

        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(merchantId, "merchantId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");

        if (totalAmount.signum() < 0)
            throw new IllegalArgumentException(
                    "Payment outcome amount must not be negative");
    }

    private void applyOutcome(
            UUID orderId,
            UUID merchantId,
            UUID customerId,
            BigDecimal totalAmount,
            OrderStatus targetStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found for payment outcome: " + orderId));

        validateOrderMatchesEvent(order, merchantId, customerId, totalAmount);

        // Redelivery of the same logical outcome is safe.
        if (order.status() == targetStatus)
            return;

        if (order.status() != OrderStatus.CREATED) {
            throw new IllegalStateException("Conflicting payment outcome for order "
                    + orderId + ": current status=" + order.status()
                    + ", requested status=" + targetStatus);
        }

        boolean transitioned = orderRepository
                .transitionStatus(orderId, OrderStatus.CREATED, targetStatus);

        if (transitioned)
            return;

        // Another consumer or request may have won the conditional update.
        Order latestOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found after payment outcome transition: " + orderId));

        validateOrderMatchesEvent(latestOrder, merchantId, customerId, totalAmount);

        if (latestOrder.status() == targetStatus)
            return;

        throw new IllegalStateException("Payment outcome transition conflict for order "
                + orderId + ": current status=" + latestOrder.status()
                + ", requested status=" + targetStatus);
    }

    private void validateOrderMatchesEvent(
            Order order,
            UUID merchantId,
            UUID customerId,
            BigDecimal totalAmount) {

        if (!order.merchantId().equals(merchantId)
                || !order.customerId().equals(customerId)
                || order.totalAmount().compareTo(totalAmount) != 0) {

            throw new IllegalArgumentException(
                    "Payment outcome does not match persisted order " + order.id());
        }
    }
}