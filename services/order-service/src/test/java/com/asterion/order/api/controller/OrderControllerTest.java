package com.asterion.order.api.controller;

import com.asterion.order.application.exception.MerchantNotActiveException;
import com.asterion.order.application.exception.MerchantNotFoundException;
import com.asterion.order.application.exception.MerchantServiceException;
import com.asterion.order.application.port.in.CreateOrderUseCase;
import com.asterion.order.domain.model.Order;
import com.asterion.order.infrastructure.security.OrderUserIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID MERCHANT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private CreateOrderUseCase createOrderUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createOrderUseCase = mock(CreateOrderUseCase.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(createOrderUseCase))
                .build();
    }

    @Test
    void shouldCreateOrderUsingTrustedUserIdentity() throws Exception {
        Order order = Order.create(MERCHANT_ID, USER_ID, new BigDecimal("125.50"));

        when(createOrderUseCase.create(any())).thenReturn(order);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER"))
                        .content(
                                """
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """
                        ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID.toString()))
                .andExpect(jsonPath("$.customerId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.totalAmount").value(125.50))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(createOrderUseCase).create(any());
    }

    @Test
    void shouldRejectMissingTrustedUserIdentity() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(
                                """
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """
                        ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void shouldRejectMissingMerchantId() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER"))
                        .content(
                                """
                                {
                                  "totalAmount": 125.50
                                }
                                """
                        ))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void shouldRejectMissingTotalAmount() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER"))
                        .content(
                                """
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222"
                                }
                                """
                        ))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void shouldIgnoreCustomerIdSuppliedByClient() throws Exception {

        Order order = Order.create(MERCHANT_ID, USER_ID, new BigDecimal("100.00"));

        when(createOrderUseCase.create(any())).thenReturn(order);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER"))
                        .content(
                                """
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "customerId":
                                    "99999999-9999-9999-9999-999999999999",
                                  "totalAmount": 100.00
                                }
                                """
                        ))
                .andExpect(status().isCreated());

        verify(createOrderUseCase).create(any());
    }

    @Test
    void shouldReturnNotFoundWhenMerchantDoesNotExist() throws Exception {
        when(createOrderUseCase.create(any()))
                .thenThrow(new MerchantNotFoundException(MERCHANT_ID));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER"))
                        .content(
                                """
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """
                        ))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenMerchantIsNotActive() throws Exception {
        when(createOrderUseCase.create(any()))
                .thenThrow(new MerchantNotActiveException(MERCHANT_ID, "SUSPENDED"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER"))
                        .content(
                                """
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """
                        ))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnBadGatewayWhenMerchantServiceFails() throws Exception {
        when(createOrderUseCase.create(any()))
                .thenThrow(new MerchantServiceException("Merchant Service is unavailable"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER"))
                        .content(
                                """
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """
                        ))
                .andExpect(status().isBadGateway());
    }
}