package com.asterion.order.application.port.in;

import com.asterion.order.application.command.GetOrderCommand;
import com.asterion.order.domain.model.Order;

public interface GetOrderUseCase {

    Order get(GetOrderCommand command);
}