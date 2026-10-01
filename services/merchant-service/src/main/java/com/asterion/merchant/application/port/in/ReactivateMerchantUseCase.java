package com.asterion.merchant.application.port.in;

import com.asterion.merchant.application.command.ReactivateMerchantCommand;
import com.asterion.merchant.domain.model.Merchant;

public interface ReactivateMerchantUseCase {

    Merchant reactivate(ReactivateMerchantCommand command);
}