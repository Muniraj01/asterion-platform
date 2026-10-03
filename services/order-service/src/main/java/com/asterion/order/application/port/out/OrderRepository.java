package com.asterion.order.application.port.out;

import com.asterion.order.domain.model.Order;

import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    boolean existsById(UUID orderId);
}