package com.asterion.order.api.controller;

import com.asterion.order.api.request.CreateOrderRequest;
import com.asterion.order.api.response.OrderResponse;
import com.asterion.order.application.command.CreateOrderCommand;
import com.asterion.order.application.exception.MerchantNotActiveException;
import com.asterion.order.application.exception.MerchantNotFoundException;
import com.asterion.order.application.exception.MerchantServiceException;
import com.asterion.order.application.port.in.CreateOrderUseCase;
import com.asterion.order.domain.model.Order;
import com.asterion.order.infrastructure.security.OrderUserIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
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
            Order order = createOrderUseCase.create(
                    new CreateOrderCommand(
                            request.merchantId(), identity.userId(), request.totalAmount()
                    )
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
}