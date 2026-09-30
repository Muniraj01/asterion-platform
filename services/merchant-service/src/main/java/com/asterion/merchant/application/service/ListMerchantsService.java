package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.ListMerchantsCommand;
import com.asterion.merchant.application.model.MerchantPage;
import com.asterion.merchant.application.port.in.ListMerchantsUseCase;
import com.asterion.merchant.application.port.out.MerchantRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public class ListMerchantsService implements ListMerchantsUseCase {

    private final MerchantRepository merchantRepository;

    public ListMerchantsService(MerchantRepository merchantRepository) {
        this.merchantRepository = Objects.requireNonNull(
                merchantRepository, "merchantRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantPage list(ListMerchantsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.authenticatedUserId(),
                "authenticatedUserId must not be null");

        if (command.page() < 0)
            throw new IllegalArgumentException("page must not be negative");

        if (command.size() <= 0)
            throw new IllegalArgumentException("size must be greater than zero");

        return merchantRepository.findByOwnerUserId(
                command.authenticatedUserId(), command.page(), command.size()
        );
    }
}