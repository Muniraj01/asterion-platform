package com.asterion.merchant.application.port.out;

import com.asterion.merchant.domain.model.Merchant;

public interface MerchantRepository {

    Merchant save(Merchant merchant);
}