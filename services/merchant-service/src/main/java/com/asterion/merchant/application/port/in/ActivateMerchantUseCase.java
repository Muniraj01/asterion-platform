package com.asterion.merchant.application.port.in;

import com.asterion.merchant.application.command.ActivateMerchantCommand;
import com.asterion.merchant.domain.model.Merchant;

public interface ActivateMerchantUseCase {

    Merchant activate(ActivateMerchantCommand command);
}