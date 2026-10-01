package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.SuspendMerchantCommand;
import com.asterion.merchant.application.event.MerchantSuspendedEvent;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.in.SuspendMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class SuspendMerchantService implements SuspendMerchantUseCase {

    private static final String EVENT_TYPE = "merchant.suspended.v1";
    private static final String NEW_STATUS = "NEW";

    private final MerchantRepository merchantRepository;
    private final MerchantOutboxRepository merchantOutboxRepository;
    private final ObjectMapper objectMapper;

    public SuspendMerchantService(
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
    public Merchant suspend(SuspendMerchantCommand command) {
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

        merchant.suspend();
        Merchant suspendedMerchant = merchantRepository.save(merchant);

        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        MerchantSuspendedEvent event = new MerchantSuspendedEvent(
                eventId,
                suspendedMerchant.id(),
                suspendedMerchant.ownerUserId(),
                occurredAt
        );

        MerchantOutboxEvent outboxEvent = new MerchantOutboxEvent(
                eventId,
                suspendedMerchant.id(),
                EVENT_TYPE,
                serialize(event),
                occurredAt,
                NEW_STATUS,
                null
        );

        merchantOutboxRepository.save(outboxEvent);
        return suspendedMerchant;
    }

    private String serialize(MerchantSuspendedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize MerchantSuspendedEvent", exception);
        }
    }
}