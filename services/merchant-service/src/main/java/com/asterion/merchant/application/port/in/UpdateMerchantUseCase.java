package com.asterion.merchant.application.port.in;

import com.asterion.merchant.application.command.UpdateMerchantCommand;
import com.asterion.merchant.domain.model.Merchant;

public interface UpdateMerchantUseCase {

    Merchant update(UpdateMerchantCommand command);
}