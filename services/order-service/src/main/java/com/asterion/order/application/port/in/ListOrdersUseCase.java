package com.asterion.order.application.port.in;

import com.asterion.order.application.command.ListOrdersCommand;
import com.asterion.order.application.model.OrderPage;

public interface ListOrdersUseCase {

    OrderPage list(ListOrdersCommand command);
}