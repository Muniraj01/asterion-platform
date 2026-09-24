package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.ActivateMerchantCommand;
import com.asterion.merchant.application.event.MerchantActivatedEvent;
import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.in.ActivateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class ActivateMerchantService implements ActivateMerchantUseCase {

    private static final String EVENT_TYPE = "merchant.activated.v1";
    private static final String NEW_STATUS = "NEW";

    private final MerchantRepository merchantRepository;
    private final MerchantOutboxRepository merchantOutboxRepository;
    private final ObjectMapper objectMapper;

    public ActivateMerchantService(MerchantRepository merchantRepository,
                                   MerchantOutboxRepository merchantOutboxRepository,
                                   ObjectMapper objectMapper) {
        this.merchantRepository = requireNonNull(
                merchantRepository, "merchantRepository must not be null");
        this.merchantOutboxRepository = requireNonNull(
                merchantOutboxRepository, "merchantOutboxRepository must not be null");
        this.objectMapper = requireNonNull(
                objectMapper, "objectMapper must not be null");
    }

    @Override
    @Transactional
    public Merchant activate(ActivateMerchantCommand command) {
        requireNonNull(command, "command must not be null");
        requireNonNull(command.merchantId(), "merchantId must not be null");

        Merchant merchant = merchantRepository.findById(command.merchantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Merchant not found: " + command.merchantId()
                ));

        merchant.activate();
        Merchant activatedMerchant = merchantRepository.save(merchant);

        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        MerchantActivatedEvent event = new MerchantActivatedEvent(
                eventId,
                activatedMerchant.id(),
                activatedMerchant.ownerUserId(),
                occurredAt
        );

        String payload = serialize(event);
        MerchantOutboxEvent outboxEvent = new MerchantOutboxEvent(
                eventId,
                activatedMerchant.id(),
                EVENT_TYPE,
                payload,
                occurredAt,
                NEW_STATUS,
                null
        );

        merchantOutboxRepository.save(outboxEvent);
        return activatedMerchant;
    }

    private String serialize(MerchantActivatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize MerchantActivatedEvent", exception);
        }
    }
}