package com.asterion.order.api.controller;

import com.asterion.order.api.response.OrderPageResponse;
import com.asterion.order.application.command.GetOrderCommand;
import com.asterion.order.application.command.ListOrdersCommand;
import com.asterion.order.application.exception.MerchantNotActiveException;
import com.asterion.order.application.exception.MerchantNotFoundException;
import com.asterion.order.application.exception.MerchantServiceException;
import com.asterion.order.application.exception.OrderOwnershipException;
import com.asterion.order.application.model.OrderPage;
import com.asterion.order.application.port.in.CreateOrderUseCase;
import com.asterion.order.application.port.in.GetOrderUseCase;
import com.asterion.order.application.port.in.ListOrdersUseCase;
import com.asterion.order.domain.model.Order;
import com.asterion.order.infrastructure.security.OrderUserIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OTHER_USER_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID MERCHANT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    private CreateOrderUseCase createOrderUseCase;
    private GetOrderUseCase getOrderUseCase;
    private ListOrdersUseCase listOrdersUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        createOrderUseCase = mock(CreateOrderUseCase.class);
        getOrderUseCase = mock(GetOrderUseCase.class);
        listOrdersUseCase = mock(ListOrdersUseCase.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(
                        createOrderUseCase, getOrderUseCase, listOrdersUseCase))
                .build();
    }

    // -------------------------------------------------------------------------
    // Create Order
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateOrderUsingTrustedUserIdentity() throws Exception {
        Order order = Order.create(MERCHANT_ID, USER_ID, new BigDecimal("125.50"));

        when(createOrderUseCase.create(any())).thenReturn(order);

        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER")
                        )
                        .content("""
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """))
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
                        .content("""
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void shouldRejectMissingMerchantId() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER")
                        )
                        .content("""
                                {
                                  "totalAmount": 125.50
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createOrderUseCase);
    }

    @Test
    void shouldRejectMissingTotalAmount() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER")
                        )
                        .content("""
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222"
                                }
                                """))
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
                                        USER_ID, "customer@example.com", "USER")
                        )
                        .content("""
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "customerId":
                                    "99999999-9999-9999-9999-999999999999",
                                  "totalAmount": 100.00
                                }
                                """))
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
                                        USER_ID, "customer@example.com", "USER")
                        )
                        .content("""
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """))
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
                                        USER_ID, "customer@example.com", "USER")
                        )
                        .content("""
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """))
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
                                        USER_ID, "customer@example.com", "USER")
                        )
                        .content("""
                                {
                                  "merchantId":
                                    "22222222-2222-2222-2222-222222222222",
                                  "totalAmount": 125.50
                                }
                                """))
                .andExpect(status().isBadGateway());
    }

    // -------------------------------------------------------------------------
    // O3 - Order Retrieval
    // -------------------------------------------------------------------------

    @Test
    void shouldGetOrderUsingAuthenticatedCustomerIdentity() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order order = Order.create(MERCHANT_ID, USER_ID, new BigDecimal("125.50"));

        when(getOrderUseCase.get(any(GetOrderCommand.class))).thenReturn(order);

        mockMvc.perform(get("/api/v1/orders/{orderId}", order.id())
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.id().toString()))
                .andExpect(jsonPath("$.merchantId").value(MERCHANT_ID.toString()))
                .andExpect(jsonPath("$.customerId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.totalAmount").value(125.50))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(getOrderUseCase).get(argThat(command ->
                command.orderId().equals(order.id())
                        && command.authenticatedUserId().equals(USER_ID)
        ));
    }

    @Test
    void shouldRejectGetOrderWithoutTrustedUserIdentity() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(getOrderUseCase);
    }

    @Test
    void shouldReturnForbiddenWhenGettingOrderOwnedByAnotherCustomer() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(getOrderUseCase.get(any(GetOrderCommand.class)))
                .thenThrow(new OrderOwnershipException(
                        "Authenticated user does not own order: " + orderId
                ));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        OTHER_USER_ID, "other@example.com", "USER")
                        ))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        UUID orderId = UUID.randomUUID();

        when(getOrderUseCase.get(any(GetOrderCommand.class)))
                .thenThrow(new IllegalArgumentException("Order not found: " + orderId));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER")
                        ))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // O3 - Order Listing
    // -------------------------------------------------------------------------

    @Test
    void shouldListOrdersForAuthenticatedCustomer() throws Exception {
        Order firstOrder = Order.create(MERCHANT_ID, USER_ID, new BigDecimal("125.50"));
        Order secondOrder = Order.create(MERCHANT_ID, USER_ID, new BigDecimal("250.00"));

        OrderPage orderPage = new OrderPage(
                List.of(firstOrder, secondOrder),
                0,
                20,
                2,
                1
        );

        when(listOrdersUseCase.list(any(ListOrdersCommand.class))).thenReturn(orderPage);

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "20")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].customerId")
                        .value(USER_ID.toString()))
                .andExpect(jsonPath("$.content[1].customerId")
                        .value(USER_ID.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(listOrdersUseCase).list(argThat(command ->
                command.authenticatedUserId().equals(USER_ID)
                        && command.page() == 0
                        && command.size() == 20
        ));
    }

    @Test
    void shouldReturnEmptyOrderListForAuthenticatedCustomer() throws Exception {
        OrderPage orderPage = new OrderPage(
                List.of(),
                0,
                20,
                0,
                0
        );

        when(listOrdersUseCase.list(any(ListOrdersCommand.class))).thenReturn(orderPage);

        mockMvc.perform(get("/api/v1/orders")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void shouldRejectListOrdersWithoutTrustedUserIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(listOrdersUseCase);
    }

    @Test
    void shouldRejectInvalidOrderPaginationParameters() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "-1")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER")
                        ))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/orders")
                        .param("size", "0")
                        .requestAttr(OrderUserIdentity.class.getName(),
                                new OrderUserIdentity(
                                        USER_ID, "customer@example.com", "USER")
                        ))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listOrdersUseCase);
    }
}