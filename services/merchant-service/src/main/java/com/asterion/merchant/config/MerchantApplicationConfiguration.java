package com.asterion.merchant.config;

import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.application.port.out.EventPublisher;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.application.service.CreateMerchantService;
import com.asterion.merchant.application.service.MerchantOutboxPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class MerchantApplicationConfiguration {

    @Bean
    public CreateMerchantUseCase createMerchantUseCase(
            MerchantRepository merchantRepository,
            MerchantOutboxRepository merchantOutboxRepository,
            ObjectMapper objectMapper) {

        return new CreateMerchantService(
                merchantRepository,
                merchantOutboxRepository,
                objectMapper
        );
    }

    @Bean
    public MerchantOutboxPublisher merchantOutboxPublisher(
            MerchantOutboxRepository merchantOutboxRepository,
            EventPublisher eventPublisher,
            @Value("${asterion.merchant.outbox.claim-lease-seconds:60}")
            long claimLeaseSeconds) {

        return new MerchantOutboxPublisher(
                merchantOutboxRepository,
                eventPublisher,
                Clock.systemUTC(),
                Duration.ofSeconds(claimLeaseSeconds)
        );
    }
}