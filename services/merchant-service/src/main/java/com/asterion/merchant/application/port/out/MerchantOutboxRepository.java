package com.asterion.merchant.application.port.out;

import com.asterion.merchant.application.model.MerchantOutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MerchantOutboxRepository {

    MerchantOutboxEvent save(MerchantOutboxEvent event);

    List<MerchantOutboxEvent> findPending(int limit);

    List<MerchantOutboxEvent> claimPending(int limit, Instant now, Instant staleBefore);

    void markPublished(UUID eventId);
}
