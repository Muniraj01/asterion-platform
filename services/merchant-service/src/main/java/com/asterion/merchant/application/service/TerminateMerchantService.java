package com.asterion.merchant.application.service;

import com.asterion.merchant.application.command.TerminateMerchantCommand;
import com.asterion.merchant.application.event.MerchantTerminatedEvent;
import com.asterion.merchant.application.exception.MerchantOwnershipException;
import com.asterion.merchant.application.model.MerchantOutboxEvent;
import com.asterion.merchant.application.port.in.TerminateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.domain.model.Merchant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class TerminateMerchantService implements TerminateMerchantUseCase {

    private static final String EVENT_TYPE = "merchant.terminated.v1";
    private static final String NEW_STATUS = "NEW";

    private final MerchantRepository merchantRepository;
    private final MerchantOutboxRepository merchantOutboxRepository;
    private final ObjectMapper objectMapper;

    public TerminateMerchantService(
            MerchantRepository merchantRepository,
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
    public Merchant terminate(TerminateMerchantCommand command) {
        requireNonNull(command, "command must not be null");
        requireNonNull(command.merchantId(), "merchantId must not be null");
        requireNonNull(command.authenticatedUserId(),
                "authenticatedUserId must not be null");

        Merchant merchant = merchantRepository
                .findById(command.merchantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Merchant not found: " + command.merchantId()));

        if (!merchant.ownerUserId().equals(command.authenticatedUserId())) {
            throw new MerchantOwnershipException(
                    "Authenticated user does not own merchant: " + merchant.id());
        }

        merchant.terminate();
        Merchant terminatedMerchant = merchantRepository.save(merchant);

        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        MerchantTerminatedEvent event = new MerchantTerminatedEvent(
                eventId,
                terminatedMerchant.id(),
                terminatedMerchant.ownerUserId(),
                occurredAt
        );

        MerchantOutboxEvent outboxEvent = new MerchantOutboxEvent(
                eventId,
                terminatedMerchant.id(),
                EVENT_TYPE,
                serialize(event),
                occurredAt,
                NEW_STATUS,
                null
        );

        merchantOutboxRepository.save(outboxEvent);
        return terminatedMerchant;
    }

    private String serialize(MerchantTerminatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize MerchantTerminatedEvent", exception);
        }
    }
}