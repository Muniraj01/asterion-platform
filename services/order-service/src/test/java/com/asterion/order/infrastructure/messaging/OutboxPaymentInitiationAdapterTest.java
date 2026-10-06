package com.asterion.order.infrastructure.messaging;

import com.asterion.order.application.model.OrderOutboxEvent;
import com.asterion.order.application.model.PaymentInitiation;
import com.asterion.order.application.port.out.OrderOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxPaymentInitiationAdapterTest {

    private static final UUID ORDER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID MERCHANT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID CUSTOMER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private OrderOutboxRepository orderOutboxRepository;
    private ObjectMapper objectMapper;
    private OutboxPaymentInitiationAdapter adapter;

    @BeforeEach
    void setUp() {
        orderOutboxRepository = mock(OrderOutboxRepository.class);
        objectMapper = mock(ObjectMapper.class);
        adapter = new OutboxPaymentInitiationAdapter(orderOutboxRepository, objectMapper);
    }

    @Test
    void shouldCreatePaymentInitiationOutboxEvent() throws Exception {
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"event\":\"payment.initiation.requested\"}");

        adapter.initiate(new PaymentInitiation(
                ORDER_ID,
                MERCHANT_ID,
                CUSTOMER_ID,
                new BigDecimal("250.75"))
        );

        ArgumentCaptor<OrderOutboxEvent> captor =
                ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(orderOutboxRepository).save(captor.capture());

        OrderOutboxEvent event = captor.getValue();

        assertEquals(ORDER_ID, event.aggregateId());
        assertEquals("payment.initiation.requested.v1", event.eventType());
        assertEquals("{\"event\":\"payment.initiation.requested\"}", event.payload());
        assertEquals("NEW", event.status());
        assertNotNull(event.eventId());
        assertNotNull(event.createdAt());
    }

    @Test
    void shouldPropagateSerializationFailure() throws Exception {

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialization failed") {});

        assertThrows(IllegalStateException.class, () -> adapter
                .initiate(new PaymentInitiation(
                        ORDER_ID, MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00"))
                )
        );

        verifyNoInteractions(orderOutboxRepository);
    }
}