package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.CreateMerchantCommand;
import com.asterion.merchant.application.event.MerchantCreatedEvent;
import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class CreateMerchantService implements CreateMerchantUseCase {

    private final MerchantRepository merchantRepository;
    private final MerchantOutboxRepository merchantOutboxRepository;
    private final ObjectMapper objectMapper;

    public CreateMerchantService(MerchantRepository merchantRepository, 
                                 MerchantOutboxRepository merchantOutboxRepository, 
                                 ObjectMapper objectMapper) {
        this.merchantRepository = requireNonNull(merchantRepository,
                "merchantRepository must not be null");
        this.merchantOutboxRepository = requireNonNull(merchantOutboxRepository,
                "merchantOutboxRepository must not be null");
        this.objectMapper = requireNonNull(objectMapper,
                "objectMapper must not be null");
    }

    @Override
    @Transactional
    public Merchant create(CreateMerchantCommand command) {
        requireNonNull(command, "command must not be null");
        Merchant merchant = Merchant.create(
                command.ownerUserId(),
                command.businessName(),
                command.legalName(),
                command.contactEmail()
        );

        Merchant savedMerchant = merchantRepository.save(merchant);

        UUID eventId = UUID.randomUUID();
        MerchantCreatedEvent event = new MerchantCreatedEvent(
                eventId,
                savedMerchant.id(),
                savedMerchant.ownerUserId(),
                savedMerchant.businessName(),
                savedMerchant.legalName(),
                savedMerchant.contactEmail(),
                savedMerchant.createdAt()
        );

        String payload = serialize(event);
        MerchantOutboxEvent outboxEvent = new MerchantOutboxEvent(
                eventId,
                savedMerchant.id(),
                "MerchantCreated",
                payload,
                savedMerchant.createdAt(),
                "NEW"
        );

        merchantOutboxRepository.save(outboxEvent);

        return savedMerchant;
    }

    private String serialize(MerchantCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize MerchantCreatedEvent", exception);
        }
    }
}