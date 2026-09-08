package com.asterion.merchant.application.port.in;

import com.asterion.merchant.application.command.CreateMerchantCommand;
import com.asterion.merchant.domain.model.Merchant;

public interface CreateMerchantUseCase {

    Merchant create(CreateMerchantCommand command);
}