package com.asterion.merchant.application.port.in;

import com.asterion.merchant.application.command.SuspendMerchantCommand;
import com.asterion.merchant.domain.model.Merchant;

public interface SuspendMerchantUseCase {

    Merchant suspend(SuspendMerchantCommand command);
}