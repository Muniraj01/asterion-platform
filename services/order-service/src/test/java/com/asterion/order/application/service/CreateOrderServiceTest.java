package com.asterion.order.application.service;

import com.asterion.order.application.command.CreateOrderCommand;
import com.asterion.order.application.exception.MerchantNotActiveException;
import com.asterion.order.application.exception.MerchantNotFoundException;
import com.asterion.order.application.exception.MerchantServiceException;
import com.asterion.order.application.model.MerchantValidationResult;
import com.asterion.order.application.model.OrderOutboxEvent;
import com.asterion.order.application.port.out.MerchantValidationPort;
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

    private MerchantValidationPort merchantValidationPort;
    private OrderRepository orderRepository;
    private OrderOutboxRepository orderOutboxRepository;
    private ObjectMapper objectMapper;
    private CreateOrderService service;

    @BeforeEach
    void setUp() {
        merchantValidationPort = mock(MerchantValidationPort.class);
        orderRepository = mock(OrderRepository.class);
        orderOutboxRepository = mock(OrderOutboxRepository.class);
        objectMapper = mock(ObjectMapper.class);
        service = new CreateOrderService(
                merchantValidationPort,
                orderRepository,
                orderOutboxRepository,
                objectMapper
        );
    }

    @Test
    void shouldCreateOrderWhenMerchantIsActive() throws Exception {
        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenReturn(new MerchantValidationResult(MERCHANT_ID, "ACTIVE"));

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Order savedOrder = Order.create(
                MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00"));

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = service.create(new CreateOrderCommand(
                MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00"))
        );

        assertNotNull(result);
        assertEquals(savedOrder.id(), result.id());
        assertEquals(OrderStatus.CREATED, result.status());

        verify(merchantValidationPort).validate(MERCHANT_ID);
        verify(orderRepository).save(any(Order.class));
        verify(orderOutboxRepository).save(any(OrderOutboxEvent.class));
    }

    @Test
    void shouldValidateMerchantBeforePersistingOrder() throws Exception {

        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenReturn(new MerchantValidationResult(MERCHANT_ID, "ACTIVE"));

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateOrderCommand(
                MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00"))
        );

        var inOrder = inOrder(merchantValidationPort, orderRepository);
        inOrder.verify(merchantValidationPort).validate(MERCHANT_ID);
        inOrder.verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldSaveCorrectOutboxEvent() throws Exception {
        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenReturn(new MerchantValidationResult(MERCHANT_ID, "ACTIVE"));

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"event\":\"order.created\"}");

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CreateOrderCommand(
                MERCHANT_ID,
                CUSTOMER_ID,
                new BigDecimal("250.75")
        ));

        ArgumentCaptor<OrderOutboxEvent> captor =
                ArgumentCaptor.forClass(OrderOutboxEvent.class);
        verify(orderOutboxRepository).save(captor.capture());

        OrderOutboxEvent event = captor.getValue();

        assertNotNull(event.aggregateId());
        assertEquals("order.created.v1", event.eventType());
        assertEquals("{\"event\":\"order.created\"}", event.payload());
        assertEquals("NEW", event.status());

        assertNotNull(event.eventId());
        assertNotNull(event.createdAt());
    }

    @Test
    void shouldRejectNullCommand() {
        assertThrows(IllegalArgumentException.class, () -> service.create(null));

        verifyNoInteractions(
                merchantValidationPort,
                orderRepository,
                orderOutboxRepository,
                objectMapper);
    }

    @Test
    void shouldRejectNullMerchantId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateOrderCommand(
                        null, CUSTOMER_ID, new BigDecimal("10.00")))
        );

        verifyNoInteractions(
                merchantValidationPort,
                orderRepository,
                orderOutboxRepository,
                objectMapper);
    }

    @Test
    void shouldRejectNullCustomerId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, null, new BigDecimal("10.00")))
        );

        verifyNoInteractions(
                merchantValidationPort,
                orderRepository,
                orderOutboxRepository,
                objectMapper);
    }

    @Test
    void shouldRejectNullTotalAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, null))
        );

        verifyNoInteractions(
                merchantValidationPort,
                orderRepository,
                orderOutboxRepository,
                objectMapper);
    }

    @Test
    void shouldRejectInactiveMerchant() {
        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenReturn(new MerchantValidationResult(MERCHANT_ID, "SUSPENDED"));

        assertThrows(MerchantNotActiveException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID,CUSTOMER_ID, new BigDecimal("100.00")))
        );

        verify(merchantValidationPort).validate(MERCHANT_ID);

        verifyNoInteractions(
                orderRepository,
                orderOutboxRepository,
                objectMapper);
    }

    @Test
    void shouldRejectPendingMerchant() {
        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenReturn(new MerchantValidationResult(MERCHANT_ID, "PENDING"));

        assertThrows(MerchantNotActiveException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00")))
        );

        verifyNoInteractions(
                orderRepository,
                orderOutboxRepository,
                objectMapper);
    }

    @Test
    void shouldRejectTerminatedMerchant() {
        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenReturn(new MerchantValidationResult(MERCHANT_ID, "TERMINATED"));

        assertThrows(MerchantNotActiveException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00")))
        );

        verifyNoInteractions(
                orderRepository,
                orderOutboxRepository,
                objectMapper);
    }

    @Test
    void shouldPropagateMerchantNotFound() {
        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenThrow(new MerchantNotFoundException(MERCHANT_ID));

        assertThrows(MerchantNotFoundException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00")))
        );

        verifyNoInteractions(
                orderRepository,
                orderOutboxRepository,
                objectMapper);
    }

    @Test
    void shouldNotPersistWhenMerchantServiceFails() {
        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenThrow(new MerchantServiceException(
                                "Merchant Service is unavailable"));

        assertThrows(MerchantServiceException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, new BigDecimal("100.00")))
        );

        verifyNoInteractions(
                orderRepository,
                orderOutboxRepository,
                objectMapper);
    }

    @Test
    void shouldNotCreateOutboxWhenOrderPersistenceFails() throws Exception {
        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenReturn(new MerchantValidationResult(MERCHANT_ID, "ACTIVE"));

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        RuntimeException failure = new RuntimeException("database unavailable");

        when(orderRepository.save(any(Order.class))).thenThrow(failure);

        assertThrows(RuntimeException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, new BigDecimal("10.00")))
        );

        verify(orderRepository).save(any(Order.class));
        verifyNoInteractions(orderOutboxRepository);
    }

    @Test
    void shouldPropagateSerializationFailure() throws Exception {
        when(merchantValidationPort.validate(MERCHANT_ID))
                .thenReturn(new MerchantValidationResult(MERCHANT_ID, "ACTIVE"));

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("serialization failed") {});

        assertThrows(IllegalStateException.class,
                () -> service.create(new CreateOrderCommand(
                        MERCHANT_ID, CUSTOMER_ID, new BigDecimal("10.00")))
        );

        verifyNoInteractions(orderRepository, orderOutboxRepository);
    }
}