package com.asterion.order.application.port.out;

import com.asterion.order.application.model.MerchantValidationResult;

import java.util.UUID;

public interface MerchantValidationPort {

    MerchantValidationResult validate(UUID merchantId);
}