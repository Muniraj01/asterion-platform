package com.asterion.merchant.application.port.in;

import com.asterion.merchant.application.command.TerminateMerchantCommand;
import com.asterion.merchant.domain.model.Merchant;

public interface TerminateMerchantUseCase {

    Merchant terminate(TerminateMerchantCommand command);
}