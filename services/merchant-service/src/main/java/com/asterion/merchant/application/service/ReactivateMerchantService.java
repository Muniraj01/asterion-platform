package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.ReactivateMerchantCommand;
import com.asterion.merchant.application.event.MerchantReactivatedEvent;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.in.ReactivateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class ReactivateMerchantService implements ReactivateMerchantUseCase {

    private static final String EVENT_TYPE = "merchant.reactivated.v1";
    private static final String NEW_STATUS = "NEW";

    private final MerchantRepository merchantRepository;
    private final MerchantOutboxRepository merchantOutboxRepository;
    private final ObjectMapper objectMapper;

    public ReactivateMerchantService(
            MerchantRepository merchantRepository,
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
    public Merchant reactivate(ReactivateMerchantCommand command) {
        requireNonNull(command, "command must not be null");
        requireNonNull(command.merchantId(), "merchantId must not be null");
        requireNonNull(
                command.authenticatedUserId(), "authenticatedUserId must not be null");

        Merchant merchant = merchantRepository
                .findById(command.merchantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Merchant not found: " + command.merchantId()));

        if (!merchant.ownerUserId().equals(command.authenticatedUserId())) {
            throw new MerchantOwnershipException(
                    "Authenticated user does not own merchant: " + merchant.id());
        }

        merchant.reactivate();
        Merchant reactivatedMerchant = merchantRepository.save(merchant);

        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        MerchantReactivatedEvent event = new MerchantReactivatedEvent(
                eventId,
                reactivatedMerchant.id(),
                reactivatedMerchant.ownerUserId(),
                occurredAt
        );

        MerchantOutboxEvent outboxEvent = new MerchantOutboxEvent(
                eventId,
                reactivatedMerchant.id(),
                EVENT_TYPE,
                serialize(event),
                occurredAt,
                NEW_STATUS,
                null
        );

        merchantOutboxRepository.save(outboxEvent);
        return reactivatedMerchant;
    }

    private String serialize(MerchantReactivatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize MerchantReactivatedEvent", exception);
        }
    }
}