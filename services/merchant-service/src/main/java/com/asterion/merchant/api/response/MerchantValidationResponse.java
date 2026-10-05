package com.asterion.merchant.api.response;

import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;

import java.util.UUID;

public record MerchantValidationResponse(UUID merchantId,
                                         MerchantStatus status) {

    public static MerchantValidationResponse from(Merchant merchant) {
        return new MerchantValidationResponse(
                merchant.id(), merchant.status());
    }
}