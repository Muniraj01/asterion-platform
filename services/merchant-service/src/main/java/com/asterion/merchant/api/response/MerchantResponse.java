package com.asterion.merchant.api.response;

import com.asterion.merchant.domain.model.Merchant;
import com.asterion.merchant.domain.model.MerchantStatus;

import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
        UUID merchantId,
        UUID ownerUserId,
        String businessName,
        String legalName,
        String contactEmail,
        MerchantStatus status,
        Instant createdAt
) {

    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.id(),
                merchant.ownerUserId(),
                merchant.businessName(),
                merchant.legalName(),
                merchant.contactEmail(),
                merchant.status(),
                merchant.createdAt()
        );
    }
}