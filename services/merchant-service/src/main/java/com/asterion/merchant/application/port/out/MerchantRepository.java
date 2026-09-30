package com.asterion.merchant.application.port.out;

import com.asterion.merchant.application.model.MerchantPage;
import com.asterion.merchant.domain.model.Merchant;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(UUID merchantId);

    MerchantPage findByOwnerUserId(UUID ownerUserId, int page, int size);
}