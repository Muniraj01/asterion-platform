package com.asterion.merchant.application.port.out;

import com.asterion.merchant.application.model.MerchantOutboxEvent;

public interface MerchantOutboxRepository {

    MerchantOutboxEvent save(MerchantOutboxEvent event);
}