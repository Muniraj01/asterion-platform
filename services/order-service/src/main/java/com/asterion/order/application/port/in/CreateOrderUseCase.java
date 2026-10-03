package com.asterion.order.application.port.in;

import com.asterion.order.application.command.CreateOrderCommand;
import com.asterion.order.domain.model.Order;

public interface CreateOrderUseCase {

    Order create(CreateOrderCommand command);
}