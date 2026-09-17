package com.asterion.merchant.application.port.out;

import com.asterion.merchant.application.model.MerchantOutboxEvent;

import java.util.List;
import java.util.UUID;

public interface MerchantOutboxRepository {

    MerchantOutboxEvent save(MerchantOutboxEvent event);

    List<MerchantOutboxEvent> findPending(int limit);

    void markPublished(UUID eventId);
}
