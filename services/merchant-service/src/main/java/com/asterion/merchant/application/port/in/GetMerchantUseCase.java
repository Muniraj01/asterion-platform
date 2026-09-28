package com.asterion.merchant.application.port.in;

import com.asterion.merchant.application.command.GetMerchantCommand;
import com.asterion.merchant.domain.model.Merchant;

public interface GetMerchantUseCase {

    Merchant get(GetMerchantCommand command);
}