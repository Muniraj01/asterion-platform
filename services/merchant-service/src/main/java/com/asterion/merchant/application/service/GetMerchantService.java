package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.GetMerchantCommand;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.port.in.GetMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class GetMerchantService implements GetMerchantUseCase {

    private final MerchantRepository merchantRepository;

    public GetMerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = Objects.requireNonNull(
                merchantRepository, "merchantRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public Merchant get(GetMerchantCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.merchantId(), "merchantId must not be null");
        Objects.requireNonNull(
                command.authenticatedUserId(), "authenticatedUserId must not be null");

        Merchant merchant = merchantRepository.findById(command.merchantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Merchant not found: " + command.merchantId()));

        if (!merchant.ownerUserId().equals(command.authenticatedUserId())) {
            throw new MerchantOwnershipException(
                    "Authenticated user does not own merchant: " + merchant.id());
        }

        return merchant;
    }
}