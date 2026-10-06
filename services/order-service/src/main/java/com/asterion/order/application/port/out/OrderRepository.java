package com.asterion.order.application.port.out;

import com.asterion.order.domain.model.Order;
import com.asterion.order.domain.model.OrderStatus;
import com.asterion.order.application.model.OrderPage;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID orderId);

    OrderPage findByCustomerId(UUID customerId, int page, int size);

    boolean existsById(UUID orderId);

    boolean transitionStatus(UUID orderId,
                             OrderStatus expectedStatus,
                             OrderStatus targetStatus);
}