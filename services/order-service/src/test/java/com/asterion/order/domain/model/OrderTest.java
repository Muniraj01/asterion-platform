package com.asterion.order.domain.model;

import com.asterion.order.application.exception.InvalidOrderStateTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private static final UUID MERCHANT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID CUSTOMER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldCreateOrder() {
        Order order = Order.create(
                MERCHANT_ID, CUSTOMER_ID, new BigDecimal("125.50")
        );

        assertNotNull(order.id());
        assertEquals(MERCHANT_ID, order.merchantId());
        assertEquals(CUSTOMER_ID, order.customerId());
        assertEquals(new BigDecimal("125.50"), order.totalAmount());
        assertEquals(OrderStatus.CREATED, order.status());
        assertNotNull(order.createdAt());
    }

    @Test
    void shouldRejectNullMerchantId() {
        assertThrows(NullPointerException.class,
                () -> Order.create(null, CUSTOMER_ID, new BigDecimal("10.00"))
        );
    }

    @Test
    void shouldRejectNullCustomerId() {
        assertThrows(NullPointerException.class,
                () -> Order.create(MERCHANT_ID, null, new BigDecimal("10.00"))
        );
    }

    @Test
    void shouldRejectNullTotalAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.create(MERCHANT_ID, CUSTOMER_ID, null)
        );
    }

    @Test
    void shouldRejectNegativeTotalAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> Order.create(MERCHANT_ID, CUSTOMER_ID, new BigDecimal("-1.00"))
        );
    }

    @Test
    void shouldAllowZeroTotalAmount() {
        Order order = Order.create(MERCHANT_ID, CUSTOMER_ID, BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, order.totalAmount());
    }

    @Test
    void shouldCancelCreatedOrder() {
        Order order = Order.create(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00")
        );

        order.cancel();

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldCompleteCreatedOrder() {
        Order order = Order.create(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00")
        );

        order.complete();

        assertThat(order.status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void shouldRejectCancellationOfCancelledOrder() {
        Order order = Order.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                OrderStatus.CANCELLED,
                Instant.now());

        assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidOrderStateTransitionException.class);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldRejectCompletionOfCompletedOrder() {
        Order order = Order.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                OrderStatus.COMPLETED,
                Instant.now());

        assertThatThrownBy(order::complete)
                .isInstanceOf(InvalidOrderStateTransitionException.class);

        assertThat(order.status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void shouldRejectCompletionOfCancelledOrder() {
        Order order = Order.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                OrderStatus.CANCELLED,
                Instant.now());

        assertThatThrownBy(order::complete)
                .isInstanceOf(InvalidOrderStateTransitionException.class);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
    }
}