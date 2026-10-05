package com.asterion.order.application.port.out;

import com.asterion.order.application.model.OrderPage;
import com.asterion.order.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID orderId);

    OrderPage findByCustomerId(UUID customerId, int page, int size);

    boolean existsById(UUID orderId);
}