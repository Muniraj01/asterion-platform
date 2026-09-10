package com.asterion.merchant.config;

import com.asterion.merchant.application.port.in.CreateMerchantUseCase;
import com.asterion.merchant.application.port.out.MerchantOutboxRepository;
import com.asterion.merchant.application.port.out.MerchantRepository;
import com.asterion.merchant.application.service.CreateMerchantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MerchantApplicationConfiguration {

    @Bean
    public CreateMerchantUseCase createMerchantUseCase(
            MerchantRepository merchantRepository,
            MerchantOutboxRepository merchantOutboxRepository,
            ObjectMapper objectMapper) {

        return new CreateMerchantService(merchantRepository,
                merchantOutboxRepository, objectMapper);
    }
}