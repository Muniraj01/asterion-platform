package com.asterion.order.application.port.out;

import com.asterion.order.application.model.OrderOutboxEvent;

public interface EventPublisher {

    void publish(OrderOutboxEvent event);
}