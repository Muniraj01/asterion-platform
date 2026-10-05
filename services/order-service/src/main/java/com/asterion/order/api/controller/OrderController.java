package com.asterion.order.api.controller;

import com.asterion.order.api.request.CreateOrderRequest;
import com.asterion.order.api.response.OrderPageResponse;
import com.asterion.order.api.response.OrderResponse;
import com.asterion.order.application.command.CreateOrderCommand;
import com.asterion.order.application.command.GetOrderCommand;
import com.asterion.order.application.command.ListOrdersCommand;
import com.asterion.order.application.exception.MerchantNotActiveException;
import com.asterion.order.application.exception.MerchantNotFoundException;
import com.asterion.order.application.exception.MerchantServiceException;
import com.asterion.order.application.exception.OrderOwnershipException;
import com.asterion.order.application.port.in.CreateOrderUseCase;
import com.asterion.order.application.port.in.GetOrderUseCase;
import com.asterion.order.application.port.in.ListOrdersUseCase;
import com.asterion.order.application.model.OrderPage;
import com.asterion.order.domain.model.Order;
import com.asterion.order.infrastructure.security.OrderUserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           GetOrderUseCase getOrderUseCase,
                           ListOrdersUseCase listOrdersUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
        this.listOrdersUseCase = listOrdersUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {

        OrderUserIdentity identity = (OrderUserIdentity) httpRequest
                .getAttribute(OrderUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Order order = createOrderUseCase.create(new CreateOrderCommand(
                    request.merchantId(), identity.userId(), request.totalAmount())
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(OrderResponse.from(order));

        } catch (MerchantNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (MerchantNotActiveException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        } catch (MerchantServiceException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId,
                                                  HttpServletRequest httpRequest) {
        OrderUserIdentity identity = (OrderUserIdentity) httpRequest
                .getAttribute(OrderUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Order order = getOrderUseCase.get(
                    new GetOrderCommand(orderId, identity.userId())
            );
            return ResponseEntity.ok(OrderResponse.from(order));

        } catch (OrderOwnershipException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping
    public ResponseEntity<OrderPageResponse> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        OrderUserIdentity identity = (OrderUserIdentity) httpRequest
                .getAttribute(OrderUserIdentity.class.getName());

        if (identity == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (page < 0 || size <= 0)
            return ResponseEntity.badRequest().build();

        OrderPage orderPage = listOrdersUseCase.list(
                new ListOrdersCommand(identity.userId(), page, size)
        );

        return ResponseEntity.ok(OrderPageResponse.from(orderPage));
    }
}