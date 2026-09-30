package com.asterion.merchant.application.port.in;

import com.asterion.merchant.application.command.ListMerchantsCommand;
import com.asterion.merchant.application.model.MerchantPage;

public interface ListMerchantsUseCase {

    MerchantPage list(ListMerchantsCommand command);
}