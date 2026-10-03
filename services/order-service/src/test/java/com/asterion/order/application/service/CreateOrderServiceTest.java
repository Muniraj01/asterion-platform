package com.asterion.order.application.service;

import com.asterion.order.application.command.CreateOrderCommand;
import com.asterion.order.application.model.OrderOutboxEvent;
import com.asterion.order.application.port.out.OrderOutboxRepository;
import com.asterion.order.application.port.out.OrderRepository;
import com.asterion.order.domain.model.Order;
import com.asterion.order.domain.model.OrderStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateOrderServiceTest {

    private static final UUID MERCHANT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID CUSTOMER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final OrderRepository orderRepository = mock(OrderRepository.class);

    private final OrderOutboxRepository orderOutboxRepository =
            mock(OrderOutboxRepository.class);

    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    private CreateOrderService service;

    @BeforeEach
    void setUp() {
        service = new CreateOrderService(
                orderRepository, orderOutboxRepository, objectMapper);
    }

    @Test
    void shouldCreateOrder() throws Exception {
        Order savedOrder = Order.create(
                MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00")
        );

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Order result = service.create(
                new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00"))
        );

        assertEquals(savedOrder.id(), result.id());
        assertEquals(OrderStatus.CREATED, result.status());

        verify(orderRepository).save(any(Order.class));
        verify(orderOutboxRepository).save(any(OrderOutboxEvent.class));
    }

    @Test
    void shouldSaveCorrectOutboxEvent() throws Exception {
        Order savedOrder = Order.create(
                MERCHANT_ID, CUSTOMER_ID, new BigDecimal("250.75")
        );

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"event\":\"order.created\"}");

        service.create(new CreateOrderCommand(
                MERCHANT_ID, CUSTOMER_ID, new BigDecimal("250.75"))
        );

        ArgumentCaptor<OrderOutboxEvent> captor =
                ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(orderOutboxRepository).save(captor.capture());

        OrderOutboxEvent event = captor.getValue();

        assertEquals(savedOrder.id(), event.aggregateId());
        assertEquals("order.created.v1", event.eventType());
        assertEquals("{\"event\":\"order.created\"}", event.payload());
        assertEquals("NEW", event.status());
        assertNotNull(event.eventId());
        assertNotNull(event.createdAt());
    }

    @Test
    void shouldRejectNullCommand() {
        assertThrows(NullPointerException.class, () -> service.create(null));
        verifyNoInteractions(orderRepository, orderOutboxRepository, objectMapper);
    }

    @Test
    void shouldRejectNullMerchantId() {
        assertThrows(NullPointerException.class,
                () -> service.create(new CreateOrderCommand(
                        null, CUSTOMER_ID, new BigDecimal("10.00"))
                )
        );
        verifyNoInteractions(orderRepository, orderOutboxRepository, objectMapper);
    }

    @Test
    void shouldRejectNullAuthenticatedCustomerId() {
        assertThrows(NullPointerException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, null, new BigDecimal("10.00")))
        );
        verifyNoInteractions(orderRepository, orderOutboxRepository, objectMapper);
    }

    @Test
    void shouldRejectNullTotalAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, null))
        );
        verifyNoInteractions(orderRepository, orderOutboxRepository, objectMapper);
    }

    @Test
    void shouldNotCreateOutboxWhenPersistenceFails() {
        RuntimeException failure = new RuntimeException("database unavailable");
        when(orderRepository.save(any(Order.class))).thenThrow(failure);

        assertThrows(RuntimeException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, new BigDecimal("10.00")))
        );

        verify(orderRepository).save(any(Order.class));
        verifyNoInteractions(orderOutboxRepository);
        verifyNoInteractions(objectMapper);
    }

    @Test
    void shouldPropagateSerializationFailure() throws Exception {
        Order savedOrder = Order.create(
                MERCHANT_ID, CUSTOMER_ID, new BigDecimal("10.00")
        );

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        when(objectMapper
                .writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialization failed") {});

        assertThrows(IllegalStateException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, new BigDecimal("10.00")))
        );

        verify(orderRepository).save(any(Order.class));
        verifyNoInteractions(orderOutboxRepository);
    }
}