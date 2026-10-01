package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.UpdateMerchantCommand;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.port.in.UpdateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class UpdateMerchantService implements UpdateMerchantUseCase {

    private final MerchantRepository merchantRepository;

    public UpdateMerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = Objects.requireNonNull(
                merchantRepository, "merchantRepository must not be null");
    }

    @Override
    @Transactional
    public Merchant update(UpdateMerchantCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.merchantId(),
                "merchantId must not be null");
        Objects.requireNonNull(command.authenticatedUserId(),
                "authenticatedUserId must not be null");

        Merchant merchant = merchantRepository
                .findById(command.merchantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Merchant not found: " + command.merchantId()));

        if (!merchant.ownerUserId().equals(command.authenticatedUserId())) {
            throw new MerchantOwnershipException(
                    "Authenticated user does not own merchant: " + merchant.id());
        }

        merchant.updateProfile(command.businessName(),
                command.legalName(), command.contactEmail());

        return merchantRepository.save(merchant);
    }
}