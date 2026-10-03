package com.asterion.order.application.port.out;

import com.asterion.order.application.model.OrderOutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderOutboxRepository {

    OrderOutboxEvent save(OrderOutboxEvent event);

    List<OrderOutboxEvent> findPending(int limit);

    List<OrderOutboxEvent> claimPending(int limit, Instant now, Instant staleBefore);

    void markPublished(UUID eventId);
}