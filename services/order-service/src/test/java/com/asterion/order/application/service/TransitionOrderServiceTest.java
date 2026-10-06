package com.asterion.order.application.service;

import com.asterion.order.application.command.TransitionOrderCommand;
import com.asterion.order.application.exception.InvalidOrderStateTransitionException;
import com.asterion.order.application.exception.OrderOwnershipException;
import com.asterion.order.application.exception.OrderTransitionConflictException;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.domain.model.Order;
import com.asterion.order.domain.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TransitionOrderServiceTest {

    private OrderRepository orderRepository;
    private TransitionOrderService service;
    private UUID orderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        service = new TransitionOrderService(orderRepository);
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    @Test
    void shouldCancelCreatedOrder() {
        Order order = createdOrder();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        when(orderRepository.transitionStatus(
                orderId, OrderStatus.CREATED, OrderStatus.CANCELLED)
        )
                .thenReturn(true);

        Order result = service.transition(
                new TransitionOrderCommand(orderId, customerId, OrderStatus.CANCELLED)
        );

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);

        verify(orderRepository)
                .transitionStatus(orderId, OrderStatus.CREATED, OrderStatus.CANCELLED);
    }

    @Test
    void shouldCompleteCreatedOrder() {
        Order order = createdOrder();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        when(orderRepository.transitionStatus(
                orderId, OrderStatus.CREATED, OrderStatus.COMPLETED)
        )
                .thenReturn(true);

        Order result = service.transition(
                new TransitionOrderCommand(orderId, customerId, OrderStatus.COMPLETED)
        );

        assertThat(result.status()).isEqualTo(OrderStatus.COMPLETED);

        verify(orderRepository)
                .transitionStatus(orderId, OrderStatus.CREATED, OrderStatus.COMPLETED);
    }

    @Test
    void shouldRejectOrderOwnedByAnotherCustomer() {
        Order order = createdOrder();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        UUID anotherCustomer = UUID.randomUUID();

        assertThatThrownBy(() ->
                service.transition(new TransitionOrderCommand(
                        orderId, anotherCustomer, OrderStatus.CANCELLED))
        )
                .isInstanceOf(OrderOwnershipException.class);

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldRejectMissingOrder() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transition(
                new TransitionOrderCommand(orderId, customerId, OrderStatus.CANCELLED))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldRejectAlreadyCancelledOrder() {
        Order order = Order.reconstitute(
                orderId,
                UUID.randomUUID(),
                customerId,
                new BigDecimal("100.00"),
                OrderStatus.CANCELLED,
                Instant.now());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.transition(
                new TransitionOrderCommand(orderId, customerId, OrderStatus.COMPLETED))
        )
                .isInstanceOf(InvalidOrderStateTransitionException.class);

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldRejectAlreadyCompletedOrder() {
        Order order = Order.reconstitute(
                orderId,
                UUID.randomUUID(),
                customerId,
                new BigDecimal("100.00"),
                OrderStatus.COMPLETED,
                Instant.now());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.transition(
                new TransitionOrderCommand(orderId, customerId, OrderStatus.CANCELLED))
        )
                .isInstanceOf(InvalidOrderStateTransitionException.class);

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    @Test
    void shouldRejectConcurrentTransitionConflict() {
        Order order = createdOrder();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        when(orderRepository
                .transitionStatus(orderId, OrderStatus.CREATED, OrderStatus.CANCELLED)
        )
                .thenReturn(false);

        assertThatThrownBy(() -> service.transition(
                new TransitionOrderCommand(orderId, customerId, OrderStatus.CANCELLED))
        )
                .isInstanceOf(OrderTransitionConflictException.class);
    }

    @Test
    void shouldRejectCreatedAsTransitionTarget() {
        Order order = createdOrder();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.transition(
                new TransitionOrderCommand(orderId, customerId, OrderStatus.CREATED))
        )
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).transitionStatus(any(), any(), any());
    }

    private Order createdOrder() {
        return Order.reconstitute(
                orderId,
                UUID.randomUUID(),
                customerId,
                new BigDecimal("100.00"),
                OrderStatus.CREATED,
                Instant.now());
    }
}