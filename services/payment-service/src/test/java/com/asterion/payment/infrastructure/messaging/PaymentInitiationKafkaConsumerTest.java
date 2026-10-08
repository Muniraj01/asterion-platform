package com.asterion.payment.infrastructure.messaging;

import com.asterion.events.PaymentInitiationRequestedEvent;
import com.asterion.payment.application.model.PaymentInitiation;
import com.asterion.payment.application.port.in.InitiatePaymentUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PaymentInitiationKafkaConsumerTest {

    private ObjectMapper objectMapper;
    private InitiatePaymentUseCase initiatePaymentUseCase;
    private PaymentInitiationKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        initiatePaymentUseCase = mock(InitiatePaymentUseCase.class);
        consumer = new PaymentInitiationKafkaConsumer(objectMapper, initiatePaymentUseCase);
    }

    @Test
    void shouldConsumePaymentInitiationEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-10-07T10:00:00Z");

        PaymentInitiationRequestedEvent event =
                new PaymentInitiationRequestedEvent(
                        eventId,
                        occurredAt,
                        orderId,
                        merchantId,
                        customerId,
                        new BigDecimal("125.7500")
                );

        String payload = objectMapper.writeValueAsString(event);

        consumer.consume(payload);

        ArgumentCaptor<PaymentInitiation> captor =
                ArgumentCaptor.forClass(PaymentInitiation.class);

        verify(initiatePaymentUseCase).initiate(captor.capture());

        PaymentInitiation initiation = captor.getValue();

        assertEquals(eventId, initiation.eventId());
        assertEquals(occurredAt, initiation.occurredAt());
        assertEquals(orderId, initiation.orderId());
        assertEquals(merchantId, initiation.merchantId());
        assertEquals(customerId, initiation.customerId());
        assertEquals(new BigDecimal("125.7500"), initiation.totalAmount());
    }

    @Test
    void shouldRejectMalformedPayload() {
        assertThrows(IllegalStateException.class,
                () -> consumer.consume("{invalid-json"));

        verifyNoInteractions(initiatePaymentUseCase);
    }

    @Test
    void shouldPropagateApplicationFailureAsProcessingFailure() {
        PaymentInitiationRequestedEvent event =
                new PaymentInitiationRequestedEvent(
                        UUID.randomUUID(),
                        Instant.parse("2026-10-07T10:00:00Z"),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("100.00")
                );

        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }

        doThrow(new IllegalStateException("database unavailable"))
                .when(initiatePaymentUseCase)
                .initiate(any());

        assertThrows(IllegalStateException.class, () -> consumer.consume(payload));
    }
}