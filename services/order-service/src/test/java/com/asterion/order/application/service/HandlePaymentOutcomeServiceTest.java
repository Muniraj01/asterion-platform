package com.asterion.order.application.service;

import com.asterion.events.PaymentFailedEvent;
import com.asterion.events.PaymentSucceededEvent;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.domain.model.Order;
import com.asterion.order.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HandlePaymentOutcomeServiceTest {

    private OrderRepository orderRepository;
    private HandlePaymentOutcomeService service;

    private UUID orderId;
    private UUID merchantId;
    private UUID customerId;
    private UUID paymentId;
    private BigDecimal totalAmount;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        service = new HandlePaymentOutcomeService(orderRepository);

        orderId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        totalAmount = new BigDecimal("100.0000");
    }

    @Test
    void shouldCompleteCreatedOrderWhenPaymentSucceeds() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(createdOrder()));
        when(orderRepository.transitionStatus(
                orderId, OrderStatus.CREATED, OrderStatus.COMPLETED))
                .thenReturn(true);

        service.handle(paymentSucceededEvent());

        verify(orderRepository).transitionStatus(
                orderId, OrderStatus.CREATED, OrderStatus.COMPLETED);
    }

    @Test
    void shouldCancelCreatedOrderWhenPaymentFails() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(createdOrder()));
        when(orderRepository.transitionStatus(
                orderId, OrderStatus.CREATED, OrderStatus.CANCELLED))
                .thenReturn(true);

        service.handle(paymentFailedEvent());

        verify(orderRepository).transitionStatus(
                orderId, OrderStatus.CREATED, OrderStatus.CANCELLED);
    }

    @Test
    void shouldIgnoreDuplicateSuccessfulOutcome() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(orderWithStatus(OrderStatus.COMPLETED)));

        service.handle(paymentSucceededEvent());

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldIgnoreDuplicateFailedOutcome() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(orderWithStatus(OrderStatus.CANCELLED)));

        service.handle(paymentFailedEvent());

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldRejectOutcomeForUnknownOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(paymentSucceededEvent()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldRejectOutcomeWithMismatchedMerchant() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(createdOrder()));

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                UUID.randomUUID(),
                Instant.now(),
                paymentId,
                orderId,
                UUID.randomUUID(),
                customerId,
                totalAmount);

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldRejectOutcomeWithMismatchedCustomer() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(createdOrder()));

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                UUID.randomUUID(),
                Instant.now(),
                paymentId,
                orderId,
                merchantId,
                UUID.randomUUID(),
                totalAmount);

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldRejectOutcomeWithMismatchedAmount() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(createdOrder()));

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                UUID.randomUUID(),
                Instant.now(),
                paymentId,
                orderId,
                merchantId,
                customerId,
                new BigDecimal("101.00"));

        assertThatThrownBy(() -> service.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldRejectContradictoryOutcomeForTerminalOrder() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(orderWithStatus(OrderStatus.CANCELLED)));

        assertThatThrownBy(() -> service.handle(paymentSucceededEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflicting payment outcome");

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldTreatConcurrentMatchingTransitionAsSuccessfulRedelivery() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(createdOrder()))
                .thenReturn(Optional.of(orderWithStatus(OrderStatus.COMPLETED)));

        when(orderRepository.transitionStatus(
                orderId, OrderStatus.CREATED, OrderStatus.COMPLETED))
                .thenReturn(false);

        service.handle(paymentSucceededEvent());

        verify(orderRepository).transitionStatus(
                orderId, OrderStatus.CREATED, OrderStatus.COMPLETED);
        verify(orderRepository, times(2)).findById(orderId);
    }

    @Test
    void shouldRejectConcurrentConflictingTransition() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(createdOrder()))
                .thenReturn(Optional.of(orderWithStatus(OrderStatus.CANCELLED)));

        when(orderRepository.transitionStatus(
                orderId, OrderStatus.CREATED, OrderStatus.COMPLETED))
                .thenReturn(false);

        assertThatThrownBy(() -> service.handle(paymentSucceededEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment outcome transition conflict");
    }

    private PaymentSucceededEvent paymentSucceededEvent() {
        return new PaymentSucceededEvent(
                UUID.randomUUID(),
                Instant.now(),
                paymentId,
                orderId,
                merchantId,
                customerId,
                totalAmount);
    }

    private PaymentFailedEvent paymentFailedEvent() {
        return new PaymentFailedEvent(
                UUID.randomUUID(),
                Instant.now(),
                paymentId,
                orderId,
                merchantId,
                customerId,
                totalAmount);
    }

    private Order createdOrder() {
        return orderWithStatus(OrderStatus.CREATED);
    }

    private Order orderWithStatus(OrderStatus status) {
        return Order.reconstitute(
                orderId,
                merchantId,
                customerId,
                totalAmount,
                status,
                Instant.now());
    }
}