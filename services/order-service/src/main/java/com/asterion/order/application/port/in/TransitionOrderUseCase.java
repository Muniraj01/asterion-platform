package com.asterion.order.application.port.in;

import com.asterion.order.application.command.TransitionOrderCommand;
import com.asterion.order.domain.model.Order;

public interface TransitionOrderUseCase {

    Order transition(TransitionOrderCommand command);
}