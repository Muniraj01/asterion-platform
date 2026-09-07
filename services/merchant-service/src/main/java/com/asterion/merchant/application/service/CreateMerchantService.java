package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.CreateMerchantCommand;
import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;

import java.util.Objects;

public class CreateMerchantService implements CreateMerchantUseCase {

    private final MerchantRepository merchantRepository;

    public CreateMerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = Objects.requireNonNull(
                        merchantRepository, "merchantRepository must not be null");
    }

    @Override
    public Merchant create(CreateMerchantCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Merchant merchant = Merchant.create(
                command.ownerUserId(),
                command.businessName(),
                command.legalName(),
                command.contactEmail()
        );
        return merchantRepository.save(merchant);
    }
}